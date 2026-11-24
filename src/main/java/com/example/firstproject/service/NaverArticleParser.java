package com.example.firstproject.service;

import com.example.firstproject.dto.NewsArticleDto;
import com.example.firstproject.dto.NewsArticleDto.Block;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * n.news.naver.com 기사 페이지 HTML → 제목, 언론사, 입력/수정 시각, 기자, 본문(소제목·사진·중간제목·문단).
 * 기자 카드는 페이지가 열린 뒤 스크립트로 따로 불러오기 때문에, 기자 이름·이메일은 본문 문구에서 찾습니다.
 */
final class NaverArticleParser {

    private NaverArticleParser() {}

    private static final Set<String> SKIP = Set.of("script", "style", "iframe", "noscript", "button", "table", "video", "audio", "input");
    private static final Set<String> BLOCK_TAGS = Set.of("div", "p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "section", "ul", "ol", "li", "figure");
    private static final Pattern REPORTER = Pattern.compile("([가-힣]{2,4})\\s?(?:선임기자|객원기자|기자|특파원)(?=[\\s=,.)·]|$)");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    static NewsArticleDto parse(String html, String url) {
        Document doc = Jsoup.parse(html, url);

        String title = firstNonBlank(text(doc, "#title_area"), text(doc, "h2.media_end_head_headline"),
                doc.select("meta[property=og:title]").attr("content"));
        Element logo = doc.selectFirst(".media_end_head_top_logo img");
        String press = logo == null ? "" : logo.attr("alt");
        if (press.isBlank()) press = doc.select("meta[property=og:article:author]").attr("content").split("\\|")[0];
        String pressLogo = logo == null ? null : https(logo.hasAttr("data-src") ? logo.absUrl("data-src") : logo.absUrl("src"));

        List<String> categories = doc.select(".media_end_categorize_item").eachText();
        String published = firstNonBlank(doc.select("._ARTICLE_DATE_TIME").attr("data-date-time"),
                doc.select(".media_end_head_info_datestamp_time[data-date-time]").attr("data-date-time"));
        String modified = doc.select("._ARTICLE_MODIFY_DATE_TIME").attr("data-modify-date-time");
        String origin = doc.select("a.media_end_head_origin_link").attr("abs:href");
        String copyright = doc.select(".copyright .c_text").text();

        List<Block> blocks = new ArrayList<>();
        Element body = doc.selectFirst("#dic_area");
        if (body != null) {
            Walker w = new Walker(blocks);
            w.walk(body);
            w.flush();
        }

        List<String> texts = blocks.stream().filter(b -> b.type().equals("text")).map(Block::text).toList();
        return new NewsArticleDto(
                blankToNull(title), blankToNull(press.trim()), pressLogo, categories,
                blankToNull(published), blankToNull(modified),
                findReporter(texts), findEmail(texts),
                blocks, blankToNull(copyright),
                origin.startsWith("http") ? origin : null, url);
    }

    /** 앞쪽 두 문단("(서울=연합뉴스) 홍길동 기자 =")에서 먼저, 없으면 마지막 네 문단("홍길동 기자 hong@…")에서 */
    static String findReporter(List<String> texts) {
        for (String t : texts.subList(0, Math.min(2, texts.size()))) {
            Matcher m = REPORTER.matcher(t);
            if (m.find()) return m.group(0).replaceAll("\\s+", " ").trim();
        }
        String found = null;
        for (String t : texts.subList(Math.max(0, texts.size() - 4), texts.size())) {
            if (t.length() > 60) continue; // 긴 문장 속 "…기자" 는 인용일 수 있어서 짧은 끝줄만
            Matcher m = REPORTER.matcher(t);
            while (m.find()) found = m.group(0).replaceAll("\\s+", " ").trim();
        }
        return found;
    }

    static String findEmail(List<String> texts) {
        String found = null;
        for (String t : texts.subList(Math.max(0, texts.size() - 4), texts.size())) {
            Matcher m = EMAIL.matcher(t);
            while (m.find()) found = m.group();
        }
        return found;
    }

    /** 본문을 앞에서부터 훑으며 줄 단위로 문단을 만들고, 사진·소제목·중간제목은 따로 떼어 냅니다 */
    private static final class Walker {
        private final List<Block> out;
        private final StringBuilder line = new StringBuilder();
        private boolean lineBold;   // 이 줄이 처음부터 끝까지 굵은 글씨인지 (중간 제목 후보)
        private int boldDepth;

        Walker(List<Block> out) { this.out = out; }

        void walk(Element parent) {
            for (Node n : parent.childNodes()) {
                if (n instanceof TextNode t) {
                    append(t.getWholeText());
                    continue;
                }
                if (!(n instanceof Element e)) continue; // 주석 등
                String tag = e.normalName();
                if (SKIP.contains(tag) || e.hasClass("vod_player_wrap") || e.hasClass("_VOD_PLAYER_WRAP")) continue;
                if (tag.equals("br")) { flush(); continue; }

                if (e.selectFirst("img") != null && textOutsideCaption(e).length() < 20) {
                    flush();
                    image(e);
                    continue;
                }
                if (isBlock(e)) {
                    String txt = textWithBreaks(e);
                    if (txt.isEmpty()) continue;
                    if (txt.length() > 150 || e.selectFirst("img") != null) { // 긴 묶음은 안을 다시 훑음
                        flush();
                        walk(e);
                        flush();
                        continue;
                    }
                    flush();
                    boolean bodyStarted = out.stream().anyMatch(b -> !b.type().equals("subtitle"));
                    if (!bodyStarted) out.add(Block.of("subtitle", txt));
                    else out.add(Block.of("heading", txt.replace('\n', ' ')));
                    continue;
                }
                boolean bold = tag.equals("strong") || tag.equals("b");
                if (bold) boldDepth++;
                walk(e);
                if (bold) boldDepth--;
            }
        }

        private void append(String raw) {
            String t = raw.replaceAll("[\\s\\u00a0]+", " ");
            if (t.isBlank()) {
                if (!line.isEmpty()) line.append(' ');
                return;
            }
            if (line.toString().isBlank()) lineBold = boldDepth > 0;
            else if (boldDepth == 0) lineBold = false;
            line.append(t);
        }

        void flush() {
            String t = line.toString().trim();
            line.setLength(0);
            if (t.isEmpty()) return;
            boolean bodyStarted = out.stream().anyMatch(b -> !b.type().equals("subtitle"));
            out.add(Block.of(lineBold && bodyStarted && t.length() <= 80 ? "heading" : "text", t));
            lineBold = false;
        }

        private void image(Element e) {
            Element img = e.selectFirst("img");
            String src = https(img.hasAttr("data-src") ? img.absUrl("data-src") : img.absUrl("src"));
            if (src == null) return;
            Element desc = e.selectFirst(".img_desc");
            String caption = desc != null ? textWithBreaks(desc) : img.attr("alt").trim();
            out.add(new Block("image", caption.isEmpty() ? null : caption, src));
        }
    }

    private static boolean isBlock(Element e) {
        if (BLOCK_TAGS.contains(e.normalName())) return true;
        String style = e.attr("style").replace(" ", "").toLowerCase();
        return style.contains("display:block") || style.contains("border") || e.selectFirst("div, p") != null;
    }

    private static String textOutsideCaption(Element e) {
        Element c = e.clone();
        c.select(".img_desc, script, style").remove();
        return c.text().trim();
    }

    /** <br> 과 블록 태그를 줄바꿈으로 살린 글자 */
    static String textWithBreaks(Element e) {
        Element c = e.clone();
        c.select("script, style, iframe").remove();
        c.select("br").forEach(br -> br.after(new TextNode("\n")));
        c.select("p, div").forEach(d -> d.appendChild(new TextNode("\n")));
        return Arrays.stream(c.wholeText().split("\n"))
                .map(s -> s.replaceAll("[\\s\\u00a0]+", " ").trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    private static String text(Document doc, String sel) {
        Element el = doc.selectFirst(sel);
        return el == null ? "" : el.text().trim();
    }

    private static String https(String u) {
        return u != null && u.startsWith("https://") ? u : null;
    }

    private static String firstNonBlank(String... v) {
        for (String s : v) if (s != null && !s.isBlank()) return s.trim();
        return "";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
