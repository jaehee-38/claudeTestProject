package com.example.firstproject.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 한국투자증권 계좌 잔고 (모의/실전).
 */
public record KisBalanceDto(
        boolean mock,
        List<Item> items,
        BigDecimal deposit,          // 예수금
        BigDecimal totalPurchase,    // 매입금액 합계
        BigDecimal totalEvaluation,  // 유가 평가금액 합계
        BigDecimal totalProfit,      // 평가손익 합계
        BigDecimal netAsset          // 순자산
) {
    public record Item(
            String code,
            String name,
            BigDecimal quantity,
            BigDecimal averagePrice,
            BigDecimal currentPrice,
            BigDecimal purchaseAmount,
            BigDecimal evaluationAmount,
            BigDecimal profit,
            BigDecimal profitRate
    ) {}
}
