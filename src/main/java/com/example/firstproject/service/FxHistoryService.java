package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.common.TtlCache;
import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.FxHistoryDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 환율 그래프용 과거 환율.
 * 한국수출입은행 API 는 하루치씩만 줘서 10년치를 받으려면 수천 번 호출해야 하므로,
 * 기간 조회가 되는 Frankfurter(유럽중앙은행 기준환율)를 씁니다. 키가 필요 없습니다.
 * 기준환율이라 은행 매매기준율과 소수점 단위로 조금 다를 수 있습니다.
 */
@Service
@RequiredArgsConstructor
public class FxHistoryService {

    /** 그래프에 그릴 주요 통화 (원화 기준이라 한국은 선 없이 기준으로만 쓰임, 그래프 색 순서이기도 함) */
    static final List<FxHistoryDto.Currency> MAJOR = List.of(
            new FxHistoryDto.Currency("USD", "미국", "달러", 1),
            new FxHistoryDto.Currency("CNY", "중국", "위안", 1),
            new FxHistoryDto.Currency("JPY", "일본", "엔", 100),
            new FxHistoryDto.Currency("EUR", "유럽", "유로", 1));

    /** 그래프 한 줄당 최대 점 개수. 10년치(약 2,500 영업일)도 이만큼으로 솎아서 보냅니다 */
    static final int MAX_POINTS = 500;
    /** 조회 가능한 최대 기간 (10년 + 주말/연휴 여유) */
    private static final int MAX_DAYS = 3660 + 7;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final InvestProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // 기준환율은 영업일 하루 1번 바뀝니다.
    private final TtlCache<String, FxHistoryDto> cache = new TtlCache<>(Duration.ofHours(1));

    /**
     * @param from 시작일 yyyy-MM-dd
     * @param to   종료일 yyyy-MM-dd (비우면 오늘)
     */
    public FxHistoryDto history(String from, String to) {
        LocalDate today = LocalDate.now(SEOUL);
        LocalDate end = to == null || to.isBlank() ? today : parse(to, "종료일");
        LocalDate start = parse(from, "시작일");
        if (end.isAfter(today)) end = today;
        if (!start.isBefore(end)) {
            throw ApiException.badRequest("시작일은 종료일보다 앞이어야 합니다.");
        }
        if (start.isBefore(end.minusDays(MAX_DAYS))) {
            throw ApiException.badRequest("최대 10년까지 조회할 수 있습니다.");
        }
        LocalDate s = start, e = end;
        return cache.get(s + ".." + e, () -> fetch(s, e));
    }

    private FxHistoryDto fetch(LocalDate start, LocalDate end) {
        String symbols = MAJOR.stream().map(FxHistoryDto.Currency::code).collect(Collectors.joining(","));
        URI uri = UriComponentsBuilder.fromHttpUrl(props.frankfurter().url())
                .path("/" + start + ".." + end)
                .queryParam("base", "KRW")
                .queryParam("symbols", symbols)
                .build()
                .toUri();

        String body;
        try {
            body = get(uri);
        } catch (ResourceAccessException first) {
            // 가끔 응답이 늦어 타임아웃이 나서 한 번만 다시 시도합니다
            body = get(uri);
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body == null ? "" : body);
        } catch (Exception ex) {
            throw ApiException.upstream("과거 환율", "응답을 해석하지 못했습니다.");
        }
        if (root == null || !root.path("rates").isObject()) {
            String msg = root == null ? "빈 응답" : root.path("message").asText("rates 없음");
            throw ApiException.upstream("과거 환율", msg);
        }
        return toDto(root);
    }

    /**
     * 응답은 1원당 외화(예: USD 0.00074839)라서 뒤집어 "unit 단위당 원"으로 바꿉니다.
     * {"base":"KRW","start_date":"…","end_date":"…","rates":{"2026-11-17":{"USD":0.00074839,…},…}}
     */
    static FxHistoryDto toDto(JsonNode root) {
        List<String> allDates = new ArrayList<>();
        root.path("rates").fieldNames().forEachRemaining(allDates::add);
        allDates.sort(null);

        List<Integer> keep = sampleIndexes(allDates.size(), MAX_POINTS);
        List<String> dates = keep.stream().map(allDates::get).toList();

        Map<String, List<Double>> series = new LinkedHashMap<>();
        for (FxHistoryDto.Currency c : MAJOR) {
            List<Double> values = new ArrayList<>(dates.size());
            for (String d : dates) {
                JsonNode v = root.path("rates").path(d).path(c.code());
                double perKrw = v.asDouble(0);
                values.add(perKrw > 0 ? round2(c.unit() / perKrw) : null);
            }
            series.put(c.code(), values);
        }
        return new FxHistoryDto(
                "유럽중앙은행 기준환율 (Frankfurter)",
                root.path("start_date").asText(dates.isEmpty() ? null : dates.get(0)),
                root.path("end_date").asText(dates.isEmpty() ? null : dates.get(dates.size() - 1)),
                MAJOR, dates, series);
    }

    private String get(URI uri) {
        return restClient.get().uri(uri).retrieve()
                .onStatus(st -> true, (req, res) -> {})
                .body(String.class);
    }

    /** n 개 중 최대 max 개를 고르게 뽑되, 처음과 마지막은 항상 포함 */
    static List<Integer> sampleIndexes(int n, int max) {
        List<Integer> out = new ArrayList<>();
        if (n == 0) return out;
        int step = (int) Math.ceil((double) n / max);
        for (int i = 0; i < n; i += step) out.add(i);
        if (out.get(out.size() - 1) != n - 1) out.add(n - 1);
        return out;
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static LocalDate parse(String s, String what) {
        try {
            return LocalDate.parse(s == null ? "" : s.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest(what + "은 yyyy-MM-dd 형식이어야 합니다.");
        }
    }
}
