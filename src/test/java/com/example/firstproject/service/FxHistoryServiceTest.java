package com.example.firstproject.service;

import com.example.firstproject.dto.FxHistoryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FxHistoryServiceTest {

    @Test
    void toDto_invertsRatesAndUsesUnit() throws Exception {
        var root = new ObjectMapper().readTree("""
                {"amount":1.0,"base":"KRW","start_date":"2026-11-17","end_date":"2026-11-18","rates":{
                  "2026-11-18":{"USD":0.00075,"JPY":0.12},
                  "2026-11-17":{"USD":0.0008,"JPY":0.1}}}""");

        FxHistoryDto dto = FxHistoryService.toDto(root);

        assertThat(dto.dates()).containsExactly("2026-11-17", "2026-11-18"); // 날짜순 정렬
        assertThat(dto.series().get("USD")).containsExactly(1250.0, 1333.33);
        assertThat(dto.series().get("JPY")).containsExactly(1000.0, 833.33); // 100엔 기준
        assertThat(dto.series().get("EUR")).containsExactly(null, null);      // 값 없음
        assertThat(dto.currencies()).extracting(FxHistoryDto.Currency::code)
                .containsExactly("USD", "CNY", "JPY", "EUR");
    }

    @Test
    void sampleIndexes_keepsFirstAndLast() {
        assertThat(FxHistoryService.sampleIndexes(3, 500)).containsExactly(0, 1, 2);
        List<Integer> idx = FxHistoryService.sampleIndexes(2558, 500);
        assertThat(idx.size()).isLessThanOrEqualTo(501);
        assertThat(idx.get(0)).isZero();
        assertThat(idx.get(idx.size() - 1)).isEqualTo(2557);
        assertThat(FxHistoryService.sampleIndexes(0, 500)).isEmpty();
    }
}
