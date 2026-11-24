package com.example.firstproject.dto;

import java.util.List;

/**
 * 사이트 안에서 바로 보는 네이버 뉴스 기사.
 *
 * @param pressLogo     언론사 로고 이미지
 * @param categories    언론사가 분류한 섹션 (예: 경제, 세계)
 * @param publishedAt   입력 시각 "yyyy-MM-dd HH:mm:ss"
 * @param modifiedAt    수정 시각 (없으면 null)
 * @param reporter      기자 이름 (본문의 "홍길동 기자" 문구에서 찾음, 못 찾으면 null)
 * @param reporterEmail 기자 이메일 (본문 끝에서 찾음, 없으면 null)
 * @param blocks        본문을 순서대로 나눈 조각
 * @param originUrl     언론사 원문 주소
 */
public record NewsArticleDto(String title, String press, String pressLogo, List<String> categories,
                             String publishedAt, String modifiedAt, String reporter, String reporterEmail,
                             List<Block> blocks, String copyright, String originUrl, String url) {

    /**
     * @param type subtitle(소제목 상자) | heading(중간 제목) | text(문단) | image(사진, text 는 사진 설명)
     */
    public record Block(String type, String text, String src) {
        public static Block of(String type, String text) {
            return new Block(type, text, null);
        }
    }
}
