package com.example.firstproject.dto;

import java.util.List;

/**
 * 금 1kg 종목(원/g)의 기간별 시세 (그래프용). 리스트는 모두 dates 와 같은 순서.
 *
 * @param dates  영업일 (yyyy-MM-dd, 오름차순). 기간이 길면 일정 간격으로 솎아낸 날짜
 * @param close  종가 (원/g)
 * @param change 전일 대비 (원/g)
 * @param firstAvailable 이 API 가 주는 가장 오래된 날짜 (요청 기간이 이보다 앞이면 화면에 안내)
 */
public record GoldHistoryDto(
        String start,
        String end,
        String firstAvailable,
        List<String> dates,
        List<Long> close,
        List<Long> change
) {}
