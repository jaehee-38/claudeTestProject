package com.example.firstproject.service;

import com.example.firstproject.dto.GoldHistoryDto;
import com.example.firstproject.dto.GoldPriceDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoldServiceTest {

    @Test
    void toHistory_keepsOnlyMainGoldAcrossRenameAndSortsByDate() {
        List<GoldPriceDto> rows = List.of(
                row("20220502", "금 99.99_1Kg", 76700, -420),
                row("20220502", "미니금 99.99_100g", 76900, -300),
                row("20220429", "금 99.99K", 77120, 340),
                row("20220429", "미니금 100g", 77300, 200));

        GoldHistoryDto h = GoldService.toHistory(rows);

        assertThat(h.dates()).containsExactly("2022-04-29", "2022-05-02");
        assertThat(h.close()).containsExactly(77120L, 76700L);
        assertThat(h.change()).containsExactly(340L, -420L);
        assertThat(h.start()).isEqualTo("2022-04-29");
        assertThat(h.end()).isEqualTo("2022-05-02");
    }

    private static GoldPriceDto row(String date, String name, long close, long change) {
        return new GoldPriceDto(date, null, name, BigDecimal.valueOf(close), BigDecimal.valueOf(change),
                null, null, null, null, null);
    }
}
