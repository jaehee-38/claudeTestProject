package com.example.firstproject.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StockIndexParseTest {

    @Test
    void parseIndex_currentValueAndAscendingCloses() throws Exception {
        var res = new ObjectMapper().readTree("""
                {"rt_cd":"0","output1":{"bstp_nmix_prpr":"2732.86","bstp_nmix_prdy_vrss":"30.25","bstp_nmix_prdy_ctrt":"1.12"},
                 "output2":[{"stck_bsop_date":"20261124","bstp_nmix_prpr":"2732.86"},
                            {"stck_bsop_date":"20261123","bstp_nmix_prpr":"2702.61"},
                            {"stck_bsop_date":"","bstp_nmix_prpr":""}]}""");

        var d = StockService.parseIndex("KOSPI", "코스피", res);

        assertThat(d.value()).isEqualByComparingTo("2732.86");
        assertThat(d.changeRate()).isEqualByComparingTo("1.12");
        assertThat(d.dates()).containsExactly("2026-11-23", "2026-11-24");
        assertThat(d.closes()).containsExactly(new BigDecimal("2702.61"), new BigDecimal("2732.86"));
    }
}
