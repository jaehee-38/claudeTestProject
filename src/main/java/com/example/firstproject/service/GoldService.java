package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.common.Nums;
import com.example.firstproject.common.TtlCache;
import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.GoldPriceDto;
import com.example.firstproject.service.external.DataGoKr;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoldService {

    /** 보유 금 평가에 쓰는 기준 종목 (금 99.99_1kg, 가격은 원/g). 종목명 대소문자가 섞여 와서 소문자로 비교 */
    public static final String MAIN_ITEM_KEYWORD = "1kg";

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private final InvestProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // 하루 단위 데이터라 30분 캐시면 충분합니다.
    private final TtlCache<Integer, List<GoldPriceDto>> cache = new TtlCache<>(Duration.ofMinutes(30));

    /** 최근 days 일 동안의 시세, 최신순. 데이터는 다음 영업일에 올라오므로 오늘 값은 보통 없습니다. */
    public List<GoldPriceDto> recent(int days) {
        int d = Math.max(1, Math.min(days, 365));
        return cache.get(d, () -> fetch(d));
    }

    /** 보유 금 평가용 최신 1Kg 종목 시세 */
    public Optional<GoldPriceDto> latestMain() {
        return recent(14).stream()
                .filter(p -> p.itemName() != null && p.itemName().toLowerCase().contains(MAIN_ITEM_KEYWORD))
                .findFirst();
    }

    private List<GoldPriceDto> fetch(int days) {
        InvestProperties.DataGoKr cfg = props.dataGoKr();
        if (!cfg.configured()) {
            throw ApiException.notConfigured("공공데이터포털(금 시세)", "DATA_GO_KR_SERVICE_KEY");
        }

        String begin = LocalDate.now(SEOUL).minusDays(days).format(YMD);
        URI uri = UriComponentsBuilder.fromHttpUrl(cfg.goldUrl())
                .queryParam("serviceKey", DataGoKr.encodeKey(cfg.serviceKey()))
                .queryParam("resultType", "json")
                .queryParam("numOfRows", 1000)
                .queryParam("pageNo", 1)
                .queryParam("beginBasDt", begin)
                .build(true)
                .toUri();

        String body = restClient.get().uri(uri).retrieve()
                .onStatus(s -> true, (req, res) -> {}) // 상태코드와 무관하게 본문을 직접 해석
                .body(String.class);

        JsonNode root = parseJson(body);
        JsonNode header = root.path("response").path("header");
        String code = header.path("resultCode").asText();
        if (!code.equals("00")) {
            throw ApiException.upstream("금 시세", header.path("resultMsg").asText("resultCode=" + code));
        }

        JsonNode items = root.path("response").path("body").path("items").path("item");
        List<GoldPriceDto> result = new ArrayList<>();
        if (items.isArray()) {
            items.forEach(i -> result.add(toDto(i)));
        } else if (items.isObject()) { // 결과가 1건이면 배열이 아니라 객체로 옴
            result.add(toDto(items));
        }
        result.sort(Comparator.comparing(GoldPriceDto::baseDate).reversed()
                .thenComparing(GoldPriceDto::itemName, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private JsonNode parseJson(String body) {
        if (body == null || !body.strip().startsWith("{") || DataGoKr.isGatewayError(body)) {
            throw ApiException.upstream("금 시세", DataGoKr.describeError(body));
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw ApiException.upstream("금 시세", "JSON 해석 실패");
        }
    }

    private GoldPriceDto toDto(JsonNode i) {
        return new GoldPriceDto(
                i.path("basDt").asText(),
                i.path("srtnCd").asText(null),
                i.path("itmsNm").asText(null),
                Nums.parse(i.path("clpr").asText(null)),
                Nums.parse(i.path("vs").asText(null)),
                Nums.parse(i.path("fltRt").asText(null)),
                Nums.parse(i.path("mkp").asText(null)),
                Nums.parse(i.path("hipr").asText(null)),
                Nums.parse(i.path("lopr").asText(null)),
                Nums.parse(i.path("trqu").asText(null))
        );
    }
}
