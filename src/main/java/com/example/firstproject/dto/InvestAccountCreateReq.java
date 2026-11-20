package com.example.firstproject.dto;

import java.math.BigDecimal;

public record InvestAccountCreateReq(
        Long memberNo,
        String accountNm,
        BigDecimal initAmt
) {}
