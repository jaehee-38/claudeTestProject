package com.example.firstproject.dto;

import java.math.BigDecimal;

/**
 * 아파트 매매 실거래 1건 (국토교통부 실거래가 공개 API).
 *
 * @param dealAmount 거래금액 (만원)
 * @param area       전용면적 (㎡)
 * @param canceled   해제(취소)된 거래 여부
 */
public record AptTradeDto(
        String aptName,
        String dong,
        String jibun,
        String dealDate,
        Long dealAmount,
        BigDecimal area,
        Integer floor,
        Integer buildYear,
        String dealType,
        boolean canceled
) {}
