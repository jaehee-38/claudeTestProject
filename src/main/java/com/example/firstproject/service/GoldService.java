package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.common.Nums;
import com.example.firstproject.common.TtlCache;
import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.GoldHistoryDto;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final TtlCache<String, GoldHistoryDto> historyCache = new TtlCache<>(Duration.ofHours(1));

    /** 이 API 는 2020-01-02 부터 데이터가 있습니다 */
    static final LocalDate FIRST_AVAILABLE = LocalDate.of(2020, 1, 2);
    /** 그래프 한 줄당 최대 점 개수 */
    static final int MAX_POINTS = 500;
    private static final int MAX_DAYS = 3660 + 7;

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

    /**
     * 그래프용 기간 시세. 금 1kg 종목만 씁니다 (2022-05 에 종목명이 "금 99.99K" → "금 99.99_1Kg" 로 바뀌었지만
     * 같은 종목이라 이어서 그립니다. 미니금 100g 은 같은 시세라 뺍니다).
     *
     * @param from 시작일 yyyy-MM-dd
     * @param to   종료일 yyyy-MM-dd (비우면 오늘)
     */
    public GoldHistoryDto history(String from, String to) {
        LocalDate today = LocalDate.now(SEOUL);
        LocalDate end = to == null || to.isBlank() ? today : parseDate(to, "종료일");
        LocalDate start = parseDate(from, "시작일");
        if (end.isAfter(today)) end = today;
        if (!start.isBefore(end)) throw ApiException.badRequest("시작일은 종료일보다 앞이어야 합니다.");
        if (start.isBefore(end.minusDays(MAX_DAYS))) throw ApiException.badRequest("최대 10년까지 조회할 수 있습니다.");
        LocalDate s = start.isBefore(FIRST_AVAILABLE) ? FIRST_AVAILABLE : start, e = end;
        return historyCache.get(s + ".." + e, () -> fetchHistory(s, e));
    }

    private GoldHistoryDto fetchHistory(LocalDate start, LocalDate end) {
        List<GoldPriceDto> rows = new ArrayList<>();
        for (int page = 1; page <= 10; page++) { // 10년치도 약 3,400건이라 4번이면 끝납니다
            JsonNode root = request(start, end, page, 1000);
            JsonNode items = root.path("response").path("body").path("items").path("item");
            int before = rows.size();
            if (items.isArray()) items.forEach(i -> rows.add(toDto(i)));
            else if (items.isObject()) rows.add(toDto(items));
            int total = root.path("response").path("body").path("totalCount").asInt(0);
            if (rows.size() == before || rows.size() >= total) break;
        }
        return toHistory(rows);
    }

    static GoldHistoryDto toHistory(List<GoldPriceDto> rows) {
        // 날짜별로 금 1kg 종목 하나만 (미니금 제외)
        Map<String, GoldPriceDto> byDate = new LinkedHashMap<>();
        rows.stream()
                .filter(r -> r.itemName() != null && r.itemName().startsWith("금 ") && r.close() != null)
                .sorted(Comparator.comparing(GoldPriceDto::baseDate))
                .forEach(r -> byDate.putIfAbsent(r.baseDate(), r));
        List<GoldPriceDto> all = new ArrayList<>(byDate.values());

        List<String> dates = new ArrayList<>();
        List<Long> close = new ArrayList<>();
        List<Long> change = new ArrayList<>();
        for (int i : FxHistoryService.sampleIndexes(all.size(), MAX_POINTS)) {
            GoldPriceDto r = all.get(i);
            String d = r.baseDate();
            dates.add(d.length() == 8 ? d.substring(0, 4) + "-" + d.substring(4, 6) + "-" + d.substring(6) : d);
            close.add(r.close().longValue());
            change.add(r.change() == null ? null : r.change().longValue());
        }
        return new GoldHistoryDto(
                dates.isEmpty() ? null : dates.get(0),
                dates.isEmpty() ? null : dates.get(dates.size() - 1),
                FIRST_AVAILABLE.toString(), dates, close, change);
    }

    private JsonNode request(LocalDate begin, LocalDate end, int page, int size) {
        InvestProperties.DataGoKr cfg = props.dataGoKr();
        if (!cfg.configured()) {
            throw ApiException.notConfigured("공공데이터포털(금 시세)", "DATA_GO_KR_SERVICE_KEY");
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(cfg.goldUrl())
                .queryParam("serviceKey", DataGoKr.encodeKey(cfg.serviceKey()))
                .queryParam("resultType", "json")
                .queryParam("numOfRows", size)
                .queryParam("pageNo", page)
                .queryParam("beginBasDt", begin.format(YMD))
                .queryParam("endBasDt", end.format(YMD))
                .build(true)
                .toUri();
        String body = restClient.get().uri(uri).retrieve()
                .onStatus(st -> true, (req, res) -> {})
                .body(String.class);
        JsonNode root = parseJson(body);
        JsonNode header = root.path("response").path("header");
        String code = header.path("resultCode").asText();
        if (!code.equals("00")) {
            throw ApiException.upstream("금 시세", header.path("resultMsg").asText("resultCode=" + code));
        }
        return root;
    }

    private static LocalDate parseDate(String s, String what) {
        try {
            return LocalDate.parse(s == null ? "" : s.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest(what + "은 yyyy-MM-dd 형식이어야 합니다.");
        }
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

    private static GoldPriceDto toDto(JsonNode i) {
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
