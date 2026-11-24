package com.example.firstproject.service;

import com.example.firstproject.common.ApiException;
import com.example.firstproject.common.TtlCache;
import com.example.firstproject.config.InvestProperties;
import com.example.firstproject.dto.NewsArticleDto;
import com.example.firstproject.dto.NewsItemDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 홈 화면 "투자 이슈" 뉴스.
 *  - 국내: 네이버 뉴스 검색, NAVER API HUB (관련도순으로 받아 기간 안의 기사만, 모자라면 최신순으로 채움)
 *  - 해외: Google 뉴스 RSS (검색어에 when:1d / 7d / 30d 를 붙이면 기간 안의 주요 기사를 줌)
 */
@Service
@RequiredArgsConstructor
public class NewsService {

    public enum Source { NAVER, GOOGLE }

    public enum Period {
        DAY(1, "1d"), WEEK(7, "7d"), MONTH(30, "30d");
        final int days;
        final String googleWhen;
        Period(int days, String googleWhen) { this.days = days; this.googleWhen = googleWhen; }
    }

    /** 주제별 검색어 (국내, 해외) */
    static final Map<String, String[]> TOPICS = Map.of(
            "all", new String[]{"경제", "stock market OR economy"},
            "stock", new String[]{"증시", "stock market"},
            "fx", new String[]{"환율", "dollar exchange rate"},
            "gold", new String[]{"금값", "gold price"},
            "realestate", new String[]{"부동산", "housing market"});

    static final int LIMIT = 8;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final InvestProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final TtlCache<String, List<NewsItemDto>> cache = new TtlCache<>(Duration.ofMinutes(30));
    private final TtlCache<String, NewsArticleDto> articleCache = new TtlCache<>(Duration.ofMinutes(30));

    /** 본문을 가져와도 되는 주소는 네이버 뉴스 기사 페이지뿐 (다른 주소로 서버가 요청을 보내지 않도록) */
    static final Pattern NAVER_ARTICLE = Pattern.compile("^https://n\\.news\\.naver\\.com/mnews/article/\\d{3}/\\d{10}(\\?sid=\\d{3})?$");

    public List<NewsItemDto> news(String source, String period, String topic) {
        Source s = parse(Source.class, source, "source 는 naver 또는 google 입니다.");
        Period p = parse(Period.class, period, "period 는 day, week, month 중 하나입니다.");
        String t = topic == null || topic.isBlank() ? "all" : topic.trim().toLowerCase(Locale.ROOT);
        String[] q = TOPICS.get(t);
        if (q == null) throw ApiException.badRequest("topic 은 " + String.join(", ", TOPICS.keySet()) + " 중 하나입니다.");
        return cache.get(s + ":" + p + ":" + t, () -> s == Source.NAVER ? naver(q[0], p) : google(q[1], p));
    }

    // ------------------------------------------------------------------ 네이버

    private List<NewsItemDto> naver(String query, Period period) {
        InvestProperties.Naver cfg = props.naver();
        if (!cfg.configured()) {
            throw ApiException.notConfigured("네이버 검색(국내 뉴스)", "NAVER_CLIENT_ID, NAVER_CLIENT_SECRET");
        }
        OffsetDateTime since = OffsetDateTime.now(SEOUL).minusDays(period.days);
        List<NewsItemDto> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // 관련도순 = 많이 다뤄진 기사 → 기간 안의 것만. 모자라면 최신순으로 채움
        for (String sort : new String[]{"sim", "date"}) {
            for (NewsItemDto item : parseNaver(naverRequest(cfg, query, sort))) {
                if (out.size() >= LIMIT) break;
                if (isAfter(item, since) && seen.add(titleKey(item.title()))) out.add(item);
            }
            if (out.size() >= LIMIT) break;
        }
        return out;
    }

    private JsonNode naverRequest(InvestProperties.Naver cfg, String query, String sort) {
        URI uri = UriComponentsBuilder.fromHttpUrl(cfg.newsUrl())
                .queryParam("query", query)
                .queryParam("display", 100)
                .queryParam("sort", sort)
                .encode()
                .build()
                .toUri();
        // API HUB 와 예전 개발자센터는 인증 헤더 이름만 다릅니다
        boolean legacy = uri.getHost() != null && uri.getHost().equals("openapi.naver.com");
        String body = restClient.get().uri(uri)
                .header(legacy ? "X-Naver-Client-Id" : "X-NCP-APIGW-API-KEY-ID", cfg.clientId().trim())
                .header(legacy ? "X-Naver-Client-Secret" : "X-NCP-APIGW-API-KEY", cfg.clientSecret().trim())
                .retrieve()
                .onStatus(st -> true, (req, res) -> {})
                .body(String.class);
        JsonNode root;
        try {
            root = objectMapper.readTree(body == null ? "" : body);
        } catch (Exception e) {
            throw ApiException.upstream("네이버 뉴스", "응답을 해석하지 못했습니다.");
        }
        if (root == null || !root.has("items")) {
            throw ApiException.upstream("네이버 뉴스", describeNaverError(root));
        }
        return root;
    }

    /**
     * 오류는 두 가지 모양으로 옵니다.
     *  - 게이트웨이(인증 등): {"error":{"errorCode":"200","message":"Authentication Failed","details":"…"}}
     *  - 검색 파라미터:       {"errorCode":"SE02","errorMessage":"Invalid display value …"}
     */
    static String describeNaverError(JsonNode root) {
        if (root == null) return "빈 응답";
        JsonNode gw = root.path("error");
        if (gw.isObject()) {
            String details = gw.path("details").asText("");
            return gw.path("message").asText("오류") + (details.isBlank() ? "" : " - " + details)
                    + " [" + gw.path("errorCode").asText() + "]"
                    + " (Client ID/Secret 이 맞는지, Application 에 검색 API 를 추가했는지 확인하세요)";
        }
        if (root.has("errorMessage")) {
            return root.path("errorMessage").asText() + " [" + root.path("errorCode").asText() + "]";
        }
        return "items 없음";
    }

    /** {"items":[{"title":"<b>환율</b> …","originallink":"https://…","link":"https://n.news.naver.com/…","description":"…","pubDate":"Tue, 24 Nov 2026 16:35:00 +0900"}]} */
    static List<NewsItemDto> parseNaver(JsonNode root) {
        List<NewsItemDto> out = new ArrayList<>();
        for (JsonNode n : root.path("items")) {
            String original = n.path("originallink").asText("");
            String link = n.path("link").asText("");
            out.add(new NewsItemDto(
                    clean(n.path("title").asText()),
                    clean(n.path("description").asText()),
                    host(original.isBlank() ? link : original),
                    original.isBlank() ? link : original,
                    rfc1123(n.path("pubDate").asText(null)))
                    .withReaderUrl(NAVER_ARTICLE.matcher(link).matches() ? link : null));
        }
        return out;
    }

    // ------------------------------------------------------------------ 본문 보기

    public NewsArticleDto article(String url) {
        String u = url == null ? "" : url.trim();
        if (!NAVER_ARTICLE.matcher(u).matches()) {
            throw ApiException.badRequest("사이트 안에서는 네이버 뉴스 기사만 볼 수 있습니다.");
        }
        return articleCache.get(u, () -> fetchArticle(u));
    }

    private NewsArticleDto fetchArticle(String url) {
        String html;
        try {
            html = restClient.get().uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (compatible; personal-feed-reader)")
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw ApiException.upstream("네이버 뉴스", "기사를 불러오지 못했습니다.");
        }
        NewsArticleDto a = parseNaverArticle(html == null ? "" : html, url);
        if (a.blocks().stream().noneMatch(b -> b.type().equals("text"))) throw ApiException.upstream("네이버 뉴스", "기사 본문을 찾지 못했습니다.");
        return a;
    }

    static NewsArticleDto parseNaverArticle(String html, String url) {
        return NaverArticleParser.parse(html, url);
    }

    // ------------------------------------------------------------------ 구글

    private List<NewsItemDto> google(String query, Period period) {
        InvestProperties.GoogleNews cfg = props.googleNews();
        URI uri = UriComponentsBuilder.fromHttpUrl(cfg.url())
                .queryParam("q", query + " when:" + period.googleWhen)
                .queryParam("hl", cfg.hl())
                .queryParam("gl", cfg.gl())
                .queryParam("ceid", cfg.ceid())
                .encode()
                .build()
                .toUri();
        byte[] body = googleGet(uri);
        if (!isRss(body)) body = googleGet(uri); // 가끔 RSS 대신 안내 페이지가 와서 한 번만 다시 시도
        if (!isRss(body)) {
            throw ApiException.upstream("Google 뉴스", "일시적으로 RSS 가 아닌 응답이 왔습니다. 잠시 후 다시 시도해 주세요.");
        }

        List<NewsItemDto> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (NewsItemDto item : parseGoogle(body)) {
            if (out.size() >= LIMIT) break;
            if (seen.add(titleKey(item.title()))) out.add(item);
        }
        return translateTitles(out);
    }

    /** 영어 제목들을 한 번에 한국어로 번역 (줄바꿈으로 이어 보내고 다시 나눔). 실패하면 원문만 둠 */
    private List<NewsItemDto> translateTitles(List<NewsItemDto> items) {
        InvestProperties.Translate cfg = props.translate();
        if (items.isEmpty() || cfg == null || !cfg.enabled()) return items;
        try {
            String joined = String.join("\n", items.stream().map(i -> i.title().replace('\n', ' ')).toList());
            URI uri = UriComponentsBuilder.fromHttpUrl(cfg.url())
                    .queryParam("client", "gtx")
                    .queryParam("sl", "auto")
                    .queryParam("tl", "ko")
                    .queryParam("dt", "t")
                    .queryParam("q", joined)
                    .encode()
                    .build()
                    .toUri();
            String body = restClient.get().uri(uri).retrieve().body(String.class);
            List<String> ko = parseTranslation(objectMapper.readTree(body == null ? "" : body));
            if (ko.size() != items.size()) return items;
            List<NewsItemDto> out = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                String t = ko.get(i).trim();
                out.add(t.isEmpty() || t.equals(items.get(i).title()) ? items.get(i) : items.get(i).withTitleKo(t));
            }
            return out;
        } catch (Exception e) {
            return items;
        }
    }

    /** [[["번역1\n","원문1\n",…],["번역2","원문2",…]],…] → 번역문을 이어 붙여 줄 단위로 */
    static List<String> parseTranslation(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode seg : root.path(0)) sb.append(seg.path(0).asText(""));
        return List.of(sb.toString().split("\n", -1));
    }

    private byte[] googleGet(URI uri) {
        return restClient.get().uri(uri)
                .header("User-Agent", "Mozilla/5.0 (compatible; personal-feed-reader)")
                .retrieve()
                .onStatus(st -> true, (req, res) -> {})
                .body(byte[].class);
    }

    static boolean isRss(byte[] body) {
        if (body == null || body.length == 0) return false;
        String head = new String(body, 0, Math.min(body.length, 300), java.nio.charset.StandardCharsets.UTF_8);
        return head.contains("<rss");
    }

    /** RSS: <item><title>헤드라인 - 언론사</title><link>…</link><pubDate>…</pubDate><source url="…">언론사</source></item> */
    static List<NewsItemDto> parseGoogle(byte[] xml) {
        Document doc;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setExpandEntityReferences(false);
            doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw ApiException.upstream("Google 뉴스", "RSS 를 해석하지 못했습니다.");
        }
        List<NewsItemDto> out = new ArrayList<>();
        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element e = (Element) items.item(i);
            String press = text(e, "source");
            String title = text(e, "title");
            if (title != null && press != null && title.endsWith(" - " + press)) {
                title = title.substring(0, title.length() - press.length() - 3);
            }
            out.add(new NewsItemDto(title, null, press, text(e, "link"), rfc1123(text(e, "pubDate"))));
        }
        return out;
    }

    // ------------------------------------------------------------------ 공통

    private static String text(Element e, String tag) {
        NodeList nl = e.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String v = nl.item(0).getTextContent();
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** 네이버 제목/요약의 <b> 태그와 &quot; 같은 문자 정리 */
    static String clean(String s) {
        if (s == null) return null;
        return HtmlUtils.htmlUnescape(s.replaceAll("<[^>]+>", "")).trim();
    }

    static String host(String url) {
        try {
            String h = URI.create(url).getHost();
            return h == null ? null : h.replaceFirst("^(www|m|news)\\.", "");
        } catch (Exception e) {
            return null;
        }
    }

    /** "Tue, 24 Nov 2026 16:35:00 +0900" → "2026-11-24T16:35:00+09:00" (서울 시간) */
    static String rfc1123(String s) {
        if (s == null) return null;
        try {
            return ZonedDateTime.parse(s.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    .withZoneSameInstant(SEOUL).toOffsetDateTime().toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isAfter(NewsItemDto item, OffsetDateTime since) {
        if (item.publishedAt() == null) return false;
        return OffsetDateTime.parse(item.publishedAt()).isAfter(since);
    }

    /** 같은 기사를 여러 언론사가 거의 같은 제목으로 낸 경우를 하나로 (공백·기호 빼고 앞 14글자) */
    static String titleKey(String title) {
        if (title == null) return "";
        String k = title.replaceAll("[\\s\\p{Punct}“”‘’·…]", "");
        return k.length() > 14 ? k.substring(0, 14) : k;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String v, String msg) {
        try {
            return Enum.valueOf(type, v == null ? "" : v.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(msg);
        }
    }
}
