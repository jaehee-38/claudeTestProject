package com.example.firstproject.dto;

/**
 * 청약 공고 1건. 청약홈(한국부동산원)과 LH 공고를 같은 모양으로 맞춘 것.
 * 날짜는 모두 yyyy-MM-dd, 원본에 없으면 null.
 *
 * @param source      APPLYHOME(청약홈) / LH
 * @param id          출처 안에서의 공고 식별자
 * @param category    공급 구분 (예: 민영, 국민, 분양주택, 행복주택)
 * @param region      공급 지역 (예: 서울, 경기)
 * @param households  공급 세대수
 * @param receiptEnd  접수 마감일 (LH 는 공고 마감일)
 * @param status      접수예정 / 접수중 / 마감 (LH 는 원본 공고 상태를 그대로)
 * @param url         공고 상세 페이지
 */
public record SubscriptionNoticeDto(
        String source,
        String id,
        String name,
        String category,
        String region,
        String address,
        Integer households,
        String noticeDate,
        String receiptStart,
        String receiptEnd,
        String winnerDate,
        String status,
        String url
) {}
