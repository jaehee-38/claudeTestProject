package com.example.firstproject.dto;

import com.example.firstproject.entity.AssetType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 내 투자 현황: 등록한 자산을 현재 시세로 평가한 결과.
 * 현재가를 못 구한 자산은 합계에서 빠지고 priceError 에 이유가 들어갑니다.
 */
public record PortfolioDto(
        List<Item> items,
        BigDecimal totalCost,        // 평가 가능한 자산의 매입금액 합
        BigDecimal totalValue,       // 평가금액 합
        BigDecimal totalProfit,
        BigDecimal totalProfitRate,  // %
        List<Allocation> allocation, // 자산 종류별 비중
        int unpricedCount            // 현재가를 못 구한 자산 수
) {
    public record Item(
            Long id,
            AssetType assetType,
            String assetTypeLabel,
            String name,
            String code,
            BigDecimal quantity,
            BigDecimal averagePrice,
            BigDecimal manualPrice,
            String memo,
            BigDecimal currentPrice,
            String priceSource,
            String priceError,
            BigDecimal cost,
            BigDecimal value,
            BigDecimal profit,
            BigDecimal profitRate
    ) {}

    public record Allocation(AssetType assetType, String label, BigDecimal value, BigDecimal ratio) {}
}
