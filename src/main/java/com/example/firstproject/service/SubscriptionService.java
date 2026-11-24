package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.common.Nums;
import com.example.firstproject.common.TtlCache;
import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.SubscriptionListRes;
import com.example.firstproject.dto.SubscriptionNoticeDto;
import com.example.firstproject.service.external.DataGoKr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 청약 공고 모음.
 *  - 한국부동산원_청약홈 분양정보 조회 서비스 (api.odcloud.kr, JSON)
 *  - 한국토지주택공사_분양임대공고문 조회 서비스 (apis.data.go.kr, JSON)
 * 두 API 모두 공공데이터포털 서비스키 하나로 호출합니다 (API마다 활용신청은 따로).
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    public static final String APPLYHOME = "APPLYHOME";
    public static final String LH = "LH";

    /** 공고일 기준 며칠 전까지 가져올지 */
    static final int LOOKBACK_DAYS = 60;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private final InvestProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // 공고는 하루에 몇 번 바뀌지 않으므로 길게 캐시해서 호출 한도를 아낍니다.
    private final TtlCache<String, List<SubscriptionNoticeDto>> cache = new TtlCache<>(Duration.ofHours(2));

    public SubscriptionListRes notices() {
        LocalDate today = LocalDate.now(KST);
        Map<String, String> errors = new LinkedHashMap<>();
        List<SubscriptionNoticeDto> applyhome = load(APPLYHOME, errors, () -> fetchApplyhome(today));
        List<SubscriptionNoticeDto> lh = load(LH, errors, () -> fetchLh(today));
        return new SubscriptionListRes(merge(applyhome, lh), errors);
    }

    private List<SubscriptionNoticeDto> load(String source, Map<String, String> errors,
                                             java.util.function.Supplier<List<SubscriptionNoticeDto>> loader) {
        try {
            return cache.get(source, loader);
        } catch (ApiException e) {
            errors.put(source, e.getMessage());
            return List.of();
        }
    }

    // ------------------------------------------------------------------ 청약홈

    private List<SubscriptionNoticeDto> fetchApplyhome(LocalDate today) {
        InvestProperties.DataGoKr cfg = requireKey();
        String from = today.minusDays(LOOKBACK_DAYS).toString(); // yyyy-MM-dd
        List<SubscriptionNoticeDto> all = new ArrayList<>();
        int perPage = 500;
        for (int page = 1; page <= 5; page++) {
            URI uri = UriComponentsBuilder.fromHttpUrl(cfg.applyhomeUrl())
                    .queryParam("serviceKey", DataGoKr.encodeKey(cfg.serviceKey()))
                    .queryParam("page", page)
                    .queryParam("perPage", perPage)
                    .queryParam(enc("cond[RCRIT_PBLANC_DE::GTE]"), from)
                    .build(true)
                    .toUri();
            JsonNode root = getJson(uri, "청약홈");
            List<SubscriptionNoticeDto> rows = parseApplyhome(root, today);
            all.addAll(rows);
            int total = root.path("matchCount").asInt(root.path("totalCount").asInt(0));
            if (rows.isEmpty() || page * perPage >= total) break;
        }
        return all;
    }

    static List<SubscriptionNoticeDto> parseApplyhome(JsonNode root, LocalDate today) {
        List<SubscriptionNoticeDto> out = new ArrayList<>();
        for (JsonNode n : root.path("data")) {
            String start = date(text(n, "RCEPT_BGNDE"));
            String end = date(text(n, "RCEPT_ENDDE"));
            String manageNo = text(n, "HOUSE_MANAGE_NO");
            String pblancNo = text(n, "PBLANC_NO");
            out.add(new SubscriptionNoticeDto(
                    APPLYHOME,
                    manageNo + (pblancNo == null ? "" : "-" + pblancNo),
                    text(n, "HOUSE_NM"),
                    firstNonNull(text(n, "HOUSE_DTL_SECD_NM"), text(n, "HOUSE_SECD_NM")),
                    text(n, "SUBSCRPT_AREA_CODE_NM"),
                    text(n, "HSSPLY_ADRES"),
                    intOrNull(text(n, "TOT_SUPLY_HSHLDCO")),
                    date(text(n, "RCRIT_PBLANC_DE")),
                    start,
                    end,
                    date(text(n, "PRZWNER_PRESNATN_DE")),
                    statusOf(start, end, today),
                    firstNonNull(text(n, "PBLANC_URL"), text(n, "HMPG_ADRES"))));
        }
        return out;
    }

    // ------------------------------------------------------------------ LH

    private List<SubscriptionNoticeDto> fetchLh(LocalDate today) {
        InvestProperties.DataGoKr cfg = requireKey();
        List<SubscriptionNoticeDto> all = new ArrayList<>();
        int pageSize = 1000; // 60일치가 보통 500~600건이라 대부분 한 번에 끝납니다
        for (int page = 1; page <= 5; page++) {
            URI uri = UriComponentsBuilder.fromHttpUrl(cfg.lhNoticeUrl())
                    .queryParam("serviceKey", DataGoKr.encodeKey(cfg.serviceKey()))
                    .queryParam("PG_SZ", pageSize)
                    .queryParam("PAGE", page)
                    .queryParam("PAN_ST_DT", today.minusDays(LOOKBACK_DAYS).format(YMD))
                    .queryParam("PAN_ED_DT", today.format(YMD))
                    .build(true)
                    .toUri();
            JsonNode root = getJson(uri, "LH 공고");
            all.addAll(parseLh(root, today));
            // 토지·상가를 걸러낸 뒤 건수가 아니라 원본 건수로 마지막 페이지를 판단합니다
            List<JsonNode> raw = lhRows(root);
            int total = raw.isEmpty() ? 0 : intOrZero(text(raw.get(0), "ALL_CNT"));
            if (raw.size() < pageSize || page * pageSize >= total) break;
        }
        return all;
    }

    /** 청약(주택 공급)이 아닌 LH 공고: 토지 매각, 상가 임대, 어린이집 운영자 모집 */
    private static final List<String> LH_EXCLUDE = List.of("토지", "상가", "어린이집");

    /**
     * LH 응답은 [{"dsSch":[…]}, {"resHeader":[…], "dsList":[…]}] 처럼 배열 안에 섞여 오므로
     * 어디에 있든 dsList 를 찾아 읽습니다.
     */
    static List<JsonNode> lhRows(JsonNode root) {
        return root.findValues("dsList").stream().flatMap(v -> iterable(v).stream()).toList();
    }

    static List<SubscriptionNoticeDto> parseLh(JsonNode root, LocalDate today) {
        List<SubscriptionNoticeDto> out = new ArrayList<>();
        for (JsonNode n : lhRows(root)) {
            String upper = text(n, "UPP_AIS_TP_NM");
            String type = text(n, "AIS_TP_CD_NM");
            String both = (upper == null ? "" : upper) + "|" + (type == null ? "" : type);
            if (LH_EXCLUDE.stream().anyMatch(both::contains)) continue;
            String noticeDate = date(text(n, "PAN_NT_ST_DT"));
            String closeDate = date(text(n, "CLSG_DT"));
            String status = text(n, "PAN_SS");
            out.add(new SubscriptionNoticeDto(
                    LH,
                    text(n, "PAN_ID"),
                    text(n, "PAN_NM"),
                    firstNonNull(type, upper),
                    shortRegion(text(n, "CNP_CD_NM")),
                    null,
                    null, // 공고문 목록에는 세대수가 없음 (ALL_CNT 는 검색 결과 전체 건수)
                    noticeDate,
                    null,
                    closeDate,
                    null,
                    status != null ? status : statusOf(noticeDate, closeDate, today),
                    text(n, "DTL_URL")));
        }
        return out;
    }

    // ------------------------------------------------------------------ 합치기

    // LH 상태값: 접수중, 상담요청(매입임대 상시 상담), 공고중, 정정공고중, 접수마감
    private static final Map<String, Integer> STATUS_ORDER = Map.of(
            "접수중", 0, "상담요청", 0, "접수예정", 1, "공고중", 1, "정정공고중", 1);

    /** 같은 단지가 양쪽에 올라온 경우 청약홈 쪽만 남기고, 접수중 → 예정 → 마감 순으로 정렬 */
    static List<SubscriptionNoticeDto> merge(List<SubscriptionNoticeDto> applyhome, List<SubscriptionNoticeDto> lh) {
        Set<String> seen = new HashSet<>();
        List<SubscriptionNoticeDto> out = new ArrayList<>();
        for (SubscriptionNoticeDto d : applyhome) {
            if (seen.add(dedupKey(d))) out.add(d);
        }
        for (SubscriptionNoticeDto d : lh) {
            if (seen.add(dedupKey(d))) out.add(d);
        }
        out.sort(Comparator
                .comparing((SubscriptionNoticeDto d) -> STATUS_ORDER.getOrDefault(d.status(), 2))
                .thenComparing(SubscriptionNoticeDto::noticeDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    private static String dedupKey(SubscriptionNoticeDto d) {
        String name = d.name() == null ? "" : d.name().replaceAll("[\\s()\\[\\]]", "");
        return name + "|" + d.noticeDate();
    }

    static String statusOf(String start, String end, LocalDate today) {
        String t = today.toString();
        if (start != null && t.compareTo(start) < 0) return "접수예정";
        if (end != null && t.compareTo(end) > 0) return "마감";
        if (start != null || end != null) return "접수중";
        return null;
    }

    // ------------------------------------------------------------------ 공통

    private InvestProperties.DataGoKr requireKey() {
        InvestProperties.DataGoKr cfg = props.dataGoKr();
        if (!cfg.configured()) {
            throw ApiException.notConfigured("공공데이터포털(청약 공고)", "DATA_GO_KR_SERVICE_KEY");
        }
        return cfg;
    }

    private JsonNode getJson(URI uri, String source) {
        byte[] body = restClient.get().uri(uri).retrieve()
                .onStatus(s -> true, (req, res) -> {})
                .body(byte[].class);
        String text = body == null ? "" : new String(body, StandardCharsets.UTF_8);
        String trimmed = text.strip();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("[")) || DataGoKr.isGatewayError(text)) {
            throw ApiException.upstream(source, DataGoKr.describeError(text));
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(text);
        } catch (Exception e) {
            throw ApiException.upstream(source, "JSON 해석 실패");
        }
        // odcloud 오류: {"code":-4,"msg":"등록되지 않은 인증키 입니다."}
        if (root.isObject() && root.has("code") && root.has("msg") && !root.has("data")) {
            throw ApiException.upstream(source, root.path("msg").asText() + " [" + root.path("code").asText() + "]"
                    + " (활용신청 승인 전이거나 승인 직후일 수 있습니다. 보통 1시간 정도 걸립니다)");
        }
        return root;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static List<JsonNode> iterable(JsonNode v) {
        List<JsonNode> list = new ArrayList<>();
        if (v.isArray()) v.forEach(list::add);
        else if (v.isObject()) list.add(v);
        return list;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText().trim();
        return s.isEmpty() ? null : s;
    }

    /** "2024-01-15", "2024.01.15", "20240115" → "2024-01-15" */
    static String date(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("\\D", "");
        if (digits.length() < 8) return null;
        return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
    }

    // 긴 이름부터 확인해야 "전남광주통합특별시"가 "광주"보다 먼저 잡힙니다
    private static final List<Map.Entry<String, String>> REGION_NAMES = List.of(
            Map.entry("전남광주", "광주·전남"),
            Map.entry("충청북", "충북"), Map.entry("충청남", "충남"), Map.entry("충북", "충북"), Map.entry("충남", "충남"),
            Map.entry("전라북", "전북"), Map.entry("전라남", "전남"), Map.entry("전북", "전북"), Map.entry("전남", "전남"),
            Map.entry("경상북", "경북"), Map.entry("경상남", "경남"), Map.entry("경북", "경북"), Map.entry("경남", "경남"),
            Map.entry("서울", "서울"), Map.entry("부산", "부산"), Map.entry("대구", "대구"), Map.entry("인천", "인천"),
            Map.entry("광주", "광주"), Map.entry("대전", "대전"), Map.entry("울산", "울산"), Map.entry("세종", "세종"),
            Map.entry("경기", "경기"), Map.entry("강원", "강원"), Map.entry("제주", "제주"));

    /**
     * LH 지역명을 청약홈 표기에 맞춤.
     * "서울특별시" → "서울", "전북특별자치도" → "전북", "대구광역시 외" → "대구 외", "전국" → "전국"
     */
    static String shortRegion(String s) {
        if (s == null) return null;
        String name = s.trim();
        String suffix = name.endsWith(" 외") ? " 외" : "";
        for (Map.Entry<String, String> e : REGION_NAMES) {
            if (name.startsWith(e.getKey())) return e.getValue() + suffix;
        }
        return name;
    }

    private static Integer intOrNull(String s) {
        BigDecimal d = Nums.parse(s);
        return d == null ? null : d.intValue();
    }

    private static int intOrZero(String s) {
        Integer v = intOrNull(s);
        return v == null ? 0 : v;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
