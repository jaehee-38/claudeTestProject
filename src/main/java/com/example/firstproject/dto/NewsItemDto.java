package com.example.firstproject.dto;

/**
 * 뉴스 기사 1건.
 *
 * @param summary     요약 (네이버만 있음, 구글은 null)
 * @param press       언론사 (구글은 언론사명, 네이버는 원문 도메인)
 * @param publishedAt 발행 시각 ISO-8601 (예: 2026-11-24T16:35:00+09:00)
 */
public record NewsItemDto(String title, String summary, String press, String url, String publishedAt) {}
