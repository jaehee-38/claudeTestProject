package com.example.firstproject.dto;

import java.math.BigDecimal;

/**
 * 국내주식 현재가 (KIS inquire-price).
 *
 * @param sector 업종명 (KIS 시세 API 는 종목명을 주지 않아서 업종명만 표시)
 */
public record StockQuoteDto(
        String code,
        String sector,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changeRate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal volume,
        BigDecimal high52w,
        BigDecimal low52w,
        BigDecimal per,
        BigDecimal pbr
) {}
