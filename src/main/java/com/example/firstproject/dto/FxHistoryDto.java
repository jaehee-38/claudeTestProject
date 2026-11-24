package com.example.firstproject.dto;

import java.util.List;
import java.util.Map;

/**
 * 주요 통화의 기간별 원화 환율 (그래프용).
 *
 * @param dates      영업일 날짜 (yyyy-MM-dd, 오름차순). 기간이 길면 일정 간격으로 솎아낸 날짜
 * @param series     통화코드 → dates 와 같은 순서의 원화 환율 (unit 단위당 원, 값이 없는 날은 null)
 * @param currencies 표시 순서대로의 통화 정보
 */
public record FxHistoryDto(
        String source,
        String start,
        String end,
        List<Currency> currencies,
        List<String> dates,
        Map<String, List<Double>> series
) {
    /** @param unit 몇 단위당 원화인지 (엔화는 100엔 기준) */
    public record Currency(String code, String country, String name, int unit) {}
}
