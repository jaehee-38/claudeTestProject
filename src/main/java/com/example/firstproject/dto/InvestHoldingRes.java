package com.example.firstproject.dto;

import com.example.firstproject.entity.InvestAsset;
import com.example.firstproject.entity.InvestHolding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record InvestHoldingRes(
        Long accountNo,
        Long assetNo,
        String assetNm,
        BigDecimal holdQty,
        BigDecimal sellableQty,
        BigDecimal avgBuyPrc,
        BigDecimal totBuyAmt,
        BigDecimal currentPrice,
        BigDecimal evalAmt,    // holdQty * currentPrice
        BigDecimal evalPnl,    // evalAmt - totBuyAmt
        BigDecimal returnRate, // evalPnl / totBuyAmt * 100 (%)
        LocalDateTime modDt
) {
    public static InvestHoldingRes of(InvestHolding h, InvestAsset asset, BigDecimal curPrice) {
        BigDecimal evalAmt = null;
        BigDecimal evalPnl = null;
        BigDecimal returnRate = null;

        if (curPrice != null) {
            evalAmt = h.getHoldQty().multiply(curPrice).setScale(0, RoundingMode.HALF_UP);
            evalPnl = evalAmt.subtract(h.getTotBuyAmt());
            if (h.getTotBuyAmt().signum() != 0) {
                returnRate = evalPnl
                        .divide(h.getTotBuyAmt(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        return new InvestHoldingRes(
                h.getId().getAccountNo(),
                h.getId().getAssetNo(),
                asset != null ? asset.getAssetNm() : null,
                h.getHoldQty(),
                h.getSellableQty(),
                h.getAvgBuyPrc(),
                h.getTotBuyAmt(),
                curPrice,
                evalAmt,
                evalPnl,
                returnRate,
                h.getModDt()
        );
    }
}
