package com.example.firstproject.dto;

import java.util.List;
import java.util.Map;

/**
 * 청약 공고 목록. 한 출처가 실패해도(예: 아직 활용신청 승인 전) 나머지는 보여주기 위해
 * 출처별 오류 메시지를 따로 담습니다.
 *
 * @param errors 출처(APPLYHOME / LH) → 화면에 보여줄 오류 메시지
 */
public record SubscriptionListRes(List<SubscriptionNoticeDto> items, Map<String, String> errors) {}
