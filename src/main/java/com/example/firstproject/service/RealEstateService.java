package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.common.Nums;
import com.example.firstproject.common.TtlCache;
import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.AptTradeDto;
import com.example.firstproject.service.external.DataGoKr;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 국토교통부_아파트 매매 실거래가 자료 (공공데이터포털, XML 응답).
 * rt.molit.go.kr 화면을 긁어오는 대신 공식 API 를 씁니다.
 */
@Service
@RequiredArgsConstructor
public class RealEstateService {

    private static final Pattern LAWD_CD = Pattern.compile("^\\d{5}$");
    private static final Pattern DEAL_YM = Pattern.compile("^\\d{6}$");

    private final InvestProperties props;
    private final RestClient restClient;

    // 과거 달 데이터는 거의 안 바뀌므로 길게 캐시합니다.
    private final TtlCache<String, List<AptTradeDto>> cache = new TtlCache<>(Duration.ofHours(3));

    /**
     * @param lawdCd 법정동코드 앞 5자리 (시군구, 예: 11680 강남구)
     * @param dealYm 계약년월 yyyyMM
     */
    public List<AptTradeDto> aptTrades(String lawdCd, String dealYm) {
        if (lawdCd == null || !LAWD_CD.matcher(lawdCd).matches()) {
            throw ApiException.badRequest("지역코드는 5자리 숫자여야 합니다 (예: 11680).");
        }
        if (dealYm == null || !DEAL_YM.matcher(dealYm).matches()) {
            throw ApiException.badRequest("계약년월은 yyyyMM 형식이어야 합니다 (예: 202410).");
        }
        return cache.get(lawdCd + ":" + dealYm, () -> fetch(lawdCd, dealYm));
    }

    private List<AptTradeDto> fetch(String lawdCd, String dealYm) {
        InvestProperties.DataGoKr cfg = props.dataGoKr();
        if (!cfg.configured()) {
            throw ApiException.notConfigured("공공데이터포털(부동산 실거래가)", "DATA_GO_KR_SERVICE_KEY");
        }

        List<AptTradeDto> all = new ArrayList<>();
        int pageSize = 1000;
        for (int page = 1; page <= 10; page++) { // 한 구에서 한 달 1만 건을 넘는 경우는 없습니다
            Document doc = request(cfg, lawdCd, dealYm, page, pageSize);
            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                all.add(toDto((Element) items.item(i)));
            }
            int total = parseInt(textOr(doc.getDocumentElement(), "totalCount", "0"), 0);
            if (page * pageSize >= total || items.getLength() == 0) break;
        }
        all.sort(Comparator.comparing(AptTradeDto::dealDate).reversed()
                .thenComparing(AptTradeDto::aptName, Comparator.nullsLast(Comparator.naturalOrder())));
        return all;
    }

    private Document request(InvestProperties.DataGoKr cfg, String lawdCd, String dealYm, int page, int size) {
        URI uri = UriComponentsBuilder.fromHttpUrl(cfg.aptTradeUrl())
                .queryParam("serviceKey", DataGoKr.encodeKey(cfg.serviceKey()))
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYm)
                .queryParam("pageNo", page)
                .queryParam("numOfRows", size)
                .build(true)
                .toUri();

        byte[] body = restClient.get().uri(uri).retrieve()
                .onStatus(s -> true, (req, res) -> {})
                .body(byte[].class);
        String text = body == null ? "" : new String(body, StandardCharsets.UTF_8);

        if (!text.strip().startsWith("<")) {
            throw ApiException.upstream("부동산 실거래가", DataGoKr.describeError(text));
        }
        Document doc = parseXml(body);
        Element root = doc.getDocumentElement();
        if (root.getTagName().equals("OpenAPI_ServiceResponse")) {
            throw ApiException.upstream("부동산 실거래가", DataGoKr.describeError(text));
        }
        String code = textOr(root, "resultCode", "");
        if (!code.equals("00") && !code.equals("000")) {
            throw ApiException.upstream("부동산 실거래가", textOr(root, "resultMsg", "resultCode=" + code));
        }
        return doc;
    }

    private static Document parseXml(byte[] body) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            // 외부 엔티티(XXE) 차단
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setExpandEntityReferences(false);
            return f.newDocumentBuilder().parse(new ByteArrayInputStream(body));
        } catch (Exception e) {
            throw ApiException.upstream("부동산 실거래가", "XML 해석 실패");
        }
    }

    /**
     * 2024년 개편된 API 는 영문 태그(aptNm, dealAmount …)를, 예전 API 는 한글 태그(아파트, 거래금액 …)를 씁니다.
     * 둘 다 읽을 수 있게 처리합니다.
     */
    private AptTradeDto toDto(Element e) {
        String year = first(e, "dealYear", "년");
        String month = first(e, "dealMonth", "월");
        String day = first(e, "dealDay", "일");
        String date = String.format("%s-%02d-%02d",
                year == null ? "0000" : year,
                parseInt(month, 0), parseInt(day, 0));

        BigDecimal amount = Nums.parse(first(e, "dealAmount", "거래금액"));
        String cancel = first(e, "cdealType", "해제여부");

        return new AptTradeDto(
                first(e, "aptNm", "아파트"),
                first(e, "umdNm", "법정동"),
                first(e, "jibun", "지번"),
                date,
                amount == null ? null : amount.longValue(),
                Nums.parse(first(e, "excluUseAr", "전용면적")),
                parseIntOrNull(first(e, "floor", "층")),
                parseIntOrNull(first(e, "buildYear", "건축년도")),
                first(e, "dealingGbn", "거래유형"),
                cancel != null && cancel.trim().equalsIgnoreCase("O"));
    }

    private static String first(Element e, String... tags) {
        for (String t : tags) {
            NodeList nl = e.getElementsByTagName(t);
            if (nl.getLength() > 0) {
                String v = nl.item(0).getTextContent();
                if (v != null && !v.isBlank()) return v.trim();
            }
        }
        return null;
    }

    private static String textOr(Element e, String tag, String fallback) {
        String v = first(e, tag);
        return v == null ? fallback : v;
    }

    private static int parseInt(String s, int fallback) {
        Integer v = parseIntOrNull(s);
        return v == null ? fallback : v;
    }

    private static Integer parseIntOrNull(String s) {
        BigDecimal d = Nums.parse(s);
        return d == null ? null : d.intValue();
    }
}
