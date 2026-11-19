package com.example.firstproject.dto;

import java.math.BigDecimal;

/**
 * KRX 금시장 일별 시세 (원/g). 금융위원회_일반상품시세정보 getGoldPriceInfo.
 *
 * @param baseDate   기준일자 yyyyMMdd
 * @param itemName   종목명 (예: 금 99.99_1Kg, 미니금 99.99_100g)
 * @param close      종가
 * @param change     전일 대비
 * @param changeRate 등락률(%)
 */
public record GoldPriceDto(
        String baseDate,
        String itemCode,
        String itemName,
        BigDecimal close,
        BigDecimal change,
        BigDecimal changeRate,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal volume
) {}
