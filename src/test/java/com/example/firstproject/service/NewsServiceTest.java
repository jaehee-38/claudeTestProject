package com.example.firstproject.service;

import com.example.firstproject.dto.NewsItemDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NewsServiceTest {

    @Test
    void parseNaver_cleansHtmlAndUsesOriginalLink() throws Exception {
        var root = new ObjectMapper().readTree("""
                {"items":[{"title":"<b>환율</b> 1,350원 &quot;숨고르기&quot;",
                  "originallink":"https://www.hankyung.com/article/1","link":"https://n.news.naver.com/x",
                  "description":"달러 <b>환율</b>이 &amp; 내렸다","pubDate":"Tue, 24 Nov 2026 16:35:00 +0900"}]}""");

        NewsItemDto n = NewsService.parseNaver(root).get(0);

        assertThat(n.title()).isEqualTo("환율 1,350원 \"숨고르기\"");
        assertThat(n.summary()).isEqualTo("달러 환율이 & 내렸다");
        assertThat(n.press()).isEqualTo("hankyung.com");
        assertThat(n.url()).isEqualTo("https://www.hankyung.com/article/1");
        assertThat(n.publishedAt()).isEqualTo("2026-11-24T16:35+09:00");
    }

    @Test
    void parseGoogle_stripsPressFromTitle() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?><rss><channel>
                <item><title>Stocks Surge After Rate Cut - The New York Times</title>
                  <link>https://news.google.com/rss/articles/abc</link>
                  <pubDate>Tue, 24 Nov 2026 07:00:00 GMT</pubDate>
                  <source url="https://www.nytimes.com">The New York Times</source></item>
                </channel></rss>""".strip();

        List<NewsItemDto> list = NewsService.parseGoogle(xml.getBytes(StandardCharsets.UTF_8));

        assertThat(list).hasSize(1);
        assertThat(list.get(0).title()).isEqualTo("Stocks Surge After Rate Cut");
        assertThat(list.get(0).press()).isEqualTo("The New York Times");
        assertThat(list.get(0).publishedAt()).isEqualTo("2026-11-24T16:00+09:00"); // 서울 시간
    }

    @Test
    void titleKey_groupsNearDuplicates() {
        assertThat(NewsService.titleKey("[속보] 코스피, 2% 급등… 외국인 순매수"))
                .isEqualTo(NewsService.titleKey("[속보]코스피 2% 급등 외국인 순매수 지속"));
    }

    @Test
    void describeNaverError_handlesBothShapes() throws Exception {
        var om = new ObjectMapper();
        assertThat(NewsService.describeNaverError(om.readTree("""
                {"error":{"errorCode":"200","message":"Authentication Failed","details":"Authentication information are missing."}}""")))
                .startsWith("Authentication Failed - Authentication information are missing. [200]");
        assertThat(NewsService.describeNaverError(om.readTree("""
                {"errorCode":"SE02","errorMessage":"Invalid display value"}""")))
                .isEqualTo("Invalid display value [SE02]");
    }

    @Test
    void parseTranslation_joinsSegmentsAndSplitsLines() throws Exception {
        var root = new ObjectMapper().readTree("""
                [[["연준 신호에 주가 상승. ","Stocks rally. ",null],["투자자 환호\\n","Investors cheer.\\n",null],
                  ["금값 사상 최고","Gold hits record",null]],null,"en"]""");

        assertThat(NewsService.parseTranslation(root)).containsExactly("연준 신호에 주가 상승. 투자자 환호", "금값 사상 최고");
    }

    @Test
    void parseNaverArticle_buildsBlocksLikeNaverPage() {
        String html = """
                <html><head><meta property="og:article:author" content="연합뉴스 | 네이버"></head><body>
                <a class="media_end_head_top_logo"><img data-src="https://mimgnews.pstatic.net/logo.png" alt="연합뉴스"></a>
                <h2 id="title_area"><span>코스피, 2% 급등</span></h2>
                <span class="media_end_head_info_datestamp_time _ARTICLE_DATE_TIME" data-date-time="2026-11-24 15:44:07">오후 3:44</span>
                <span class="media_end_head_info_datestamp_time _ARTICLE_MODIFY_DATE_TIME" data-modify-date-time="2026-11-24 16:54:07">오후 4:54</span>
                <a class="media_end_head_origin_link" href="https://www.yna.co.kr/view/1">기사원문</a>
                <em class="media_end_categorize_item">경제</em><em class="media_end_categorize_item">세계</em>
                <article id="dic_area">
                  <strong style="display:block;border-left: 2px solid #141414;">외국인 순매수 전환<br>반도체 강세</strong><br><br>
                  <span class="end_photo_org"><div><img data-src="https://imgnews.pstatic.net/p.jpg?type&#x3D;w860" alt="대체"></div><em class="img_desc">딜링룸<br>[연합뉴스]</em></span><br><br>
                  (서울=연합뉴스) 박상돈 기자 = 첫 문단에 <b>굵은 낱말</b>이 있다.<br><br>
                  둘째&nbsp;문단.<br><br>
                  <strong><div style="border-top: 4px solid #ed6d01;">중간 제목입니다</div></strong>셋째 문단.<br><br>
                  <b>굵은 한 줄 제목</b><br>넷째 문단.<br><br>
                  <script>var x = 1;</script><iframe data-src="x"></iframe>
                  kaka@yna.co.kr<br><br>
                </article>
                <div class="copyright"><p class="c_text">Copyright ⓒ 연합뉴스.</p></div>
                </body></html>""";

        var a = NewsService.parseNaverArticle(html, "https://n.news.naver.com/mnews/article/001/0016461027?sid=104");

        assertThat(a.title()).isEqualTo("코스피, 2% 급등");
        assertThat(a.press()).isEqualTo("연합뉴스");
        assertThat(a.pressLogo()).isEqualTo("https://mimgnews.pstatic.net/logo.png");
        assertThat(a.categories()).containsExactly("경제", "세계");
        assertThat(a.publishedAt()).isEqualTo("2026-11-24 15:44:07");
        assertThat(a.modifiedAt()).isEqualTo("2026-11-24 16:54:07");
        assertThat(a.originUrl()).isEqualTo("https://www.yna.co.kr/view/1");
        assertThat(a.copyright()).isEqualTo("Copyright ⓒ 연합뉴스.");
        assertThat(a.reporter()).isEqualTo("박상돈 기자");
        assertThat(a.reporterEmail()).isEqualTo("kaka@yna.co.kr");
        assertThat(a.blocks()).extracting(b -> b.type() + ":" + b.text()).containsExactly(
                "subtitle:외국인 순매수 전환\n반도체 강세",
                "image:딜링룸\n[연합뉴스]",
                "text:(서울=연합뉴스) 박상돈 기자 = 첫 문단에 굵은 낱말이 있다.",
                "text:둘째 문단.",
                "heading:중간 제목입니다",
                "text:셋째 문단.",
                "heading:굵은 한 줄 제목",
                "text:넷째 문단.",
                "text:kaka@yna.co.kr");
        assertThat(a.blocks().get(1).src()).isEqualTo("https://imgnews.pstatic.net/p.jpg?type=w860");
    }

    @Test
    void naverArticlePattern_onlyAllowsNaverArticlePages() {
        assertThat(NewsService.NAVER_ARTICLE.matcher("https://n.news.naver.com/mnews/article/009/0005768092?sid=101").matches()).isTrue();
        assertThat(NewsService.NAVER_ARTICLE.matcher("https://n.news.naver.com/mnews/article/009/0005768092").matches()).isTrue();
        assertThat(NewsService.NAVER_ARTICLE.matcher("https://evil.com/mnews/article/009/0005768092").matches()).isFalse();
        assertThat(NewsService.NAVER_ARTICLE.matcher("https://n.news.naver.com.evil.com/mnews/article/009/0005768092").matches()).isFalse();
        assertThat(NewsService.NAVER_ARTICLE.matcher("http://169.254.169.254/latest").matches()).isFalse();
    }
}
