package com.example.firstproject.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 한국수출입은행 현재환율.
 *
 * @param date  실제 데이터 기준일 (주말·공휴일·오전 11시 이전이면 직전 영업일)
 * @param rates 통화별 환율
 */
public record ExchangeRateDto(String date, List<Rate> rates) {

    /**
     * @param code      통화코드 (USD, JPY, EUR …)
     * @param curUnit   원본 표기 (예: "JPY(100)")
     * @param unit      표기 단위 (JPY, IDR 은 100)
     * @param baseRate  매매기준율 (unit 당 원화)
     * @param ttb       전신환 받으실 때
     * @param tts       전신환 보내실 때
     */
    public record Rate(String code, String curUnit, String name, int unit,
                       BigDecimal baseRate, BigDecimal ttb, BigDecimal tts) {

        /** 통화 1단위 당 원화 */
        public BigDecimal perOneUnit() {
            if (baseRate == null) return null;
            return unit == 1 ? baseRate : baseRate.divide(BigDecimal.valueOf(unit), 6, java.math.RoundingMode.HALF_UP);
        }
    }
}
