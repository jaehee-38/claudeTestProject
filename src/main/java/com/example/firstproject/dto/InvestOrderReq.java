package com.example.firstproject.dto;

import java.math.BigDecimal;

public record InvestOrderReq(
        Long assetNo,
        String side,   // BUY / SELL
        BigDecimal qty
) {}
