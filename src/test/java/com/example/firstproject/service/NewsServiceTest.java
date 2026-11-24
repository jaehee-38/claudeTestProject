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
}
