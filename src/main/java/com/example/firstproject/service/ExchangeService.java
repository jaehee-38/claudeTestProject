package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.common.Nums;
import com.example.firstproject.common.TtlCache;
import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.ExchangeRateDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern UNIT = Pattern.compile("^([A-Z]{3})(?:\\((\\d+)\\))?$");
    /** 주말 + 연휴를 넘어갈 만큼만 거슬러 올라갑니다 (하루 호출 한도 보호) */
    private static final int MAX_LOOKBACK_DAYS = 7;

    private final InvestProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final TtlCache<String, ExchangeRateDto> cache = new TtlCache<>(Duration.ofMinutes(30));

    public ExchangeRateDto latest() {
        return cache.get("latest", this::fetchLatest);
    }

    public Optional<ExchangeRateDto.Rate> findRate(String code) {
        if (code == null) return Optional.empty();
        String c = code.trim().toUpperCase();
        return latest().rates().stream().filter(r -> r.code().equals(c)).findFirst();
    }

    private ExchangeRateDto fetchLatest() {
        InvestProperties.Koreaexim cfg = props.koreaexim();
        if (!cfg.configured()) {
            throw ApiException.notConfigured("한국수출입은행(환율)", "KOREAEXIM_AUTH_KEY");
        }
        LocalDate day = LocalDate.now(SEOUL);
        for (int i = 0; i <= MAX_LOOKBACK_DAYS; i++, day = day.minusDays(1)) {
            List<ExchangeRateDto.Rate> rates = fetchDay(cfg, day);
            if (!rates.isEmpty()) {
                return new ExchangeRateDto(day.format(DateTimeFormatter.ISO_LOCAL_DATE), rates);
            }
        }
        throw ApiException.upstream("환율", "최근 " + MAX_LOOKBACK_DAYS + "일 동안 데이터가 없습니다.");
    }

    private List<ExchangeRateDto.Rate> fetchDay(InvestProperties.Koreaexim cfg, LocalDate day) {
        URI uri = UriComponentsBuilder.fromHttpUrl(cfg.url())
                .queryParam("authkey", cfg.authKey().trim())
                .queryParam("searchdate", day.format(YMD))
                .queryParam("data", "AP01")
                .encode()
                .build()
                .toUri();

        String body = restClient.get().uri(uri).retrieve()
                .onStatus(s -> true, (req, res) -> {})
                .body(String.class);

        JsonNode root;
        try {
            if (body == null || !body.strip().startsWith("[")) {
                throw new IllegalStateException();
            }
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            String preview = body == null ? "빈 응답" : body.strip();
            if (preview.length() > 150) preview = preview.substring(0, 150) + "…";
            throw ApiException.upstream("환율", "예상하지 못한 응답: " + preview);
        }

        List<ExchangeRateDto.Rate> rates = new ArrayList<>();
        for (JsonNode n : root) {
            int result = n.path("result").asInt(1);
            if (result != 1) {
                throw ApiException.upstream("환율", switch (result) {
                    case 2 -> "DATA 코드 오류";
                    case 3 -> "인증키 오류 (KOREAEXIM_AUTH_KEY 를 확인하세요)";
                    case 4 -> "오늘 호출 한도를 다 썼습니다";
                    default -> "result=" + result;
                });
            }
            String curUnit = n.path("cur_unit").asText();
            Matcher m = UNIT.matcher(curUnit.trim());
            String code = m.matches() ? m.group(1) : curUnit.trim();
            int unit = m.matches() && m.group(2) != null ? Integer.parseInt(m.group(2)) : 1;
            rates.add(new ExchangeRateDto.Rate(
                    code, curUnit, n.path("cur_nm").asText(), unit,
                    Nums.parse(n.path("deal_bas_r").asText(null)),
                    Nums.parse(n.path("ttb").asText(null)),
                    Nums.parse(n.path("tts").asText(null))
            ));
        }
        // KRW 자기 자신은 목록에서 뺍니다.
        rates.removeIf(r -> r.code().equals("KRW"));
        return rates;
    }
}
