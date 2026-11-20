package com.example.firstproject.dto;

import com.example.firstproject.entity.InvestHolding;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvestHoldingRes(
        Long accountNo,
        Long assetNo,
        BigDecimal holdQty,
        BigDecimal sellableQty,
        BigDecimal avgBuyPrc,
        BigDecimal totBuyAmt,
        LocalDateTime modDt
) {
    public static InvestHoldingRes from(InvestHolding h) {
        return new InvestHoldingRes(
                h.getId().getAccountNo(), h.getId().getAssetNo(),
                h.getHoldQty(), h.getSellableQty(),
                h.getAvgBuyPrc(), h.getTotBuyAmt(), h.getModDt()
        );
    }
}
