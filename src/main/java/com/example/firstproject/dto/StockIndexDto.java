package com.example.firstproject.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 국내 업종지수 (KOSPI / KOSDAQ) 현재값과 최근 일별 종가.
 *
 * @param dates  영업일 (yyyy-MM-dd, 오름차순)
 * @param closes dates 와 같은 순서의 지수 종가
 */
public record StockIndexDto(
        String code,
        String name,
        BigDecimal value,
        BigDecimal change,
        BigDecimal changeRate,
        List<String> dates,
        List<BigDecimal> closes
) {}
