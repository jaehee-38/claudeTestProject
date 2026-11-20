package com.example.firstproject.dto;

import com.example.firstproject.entity.InvestAsset;

import java.math.BigDecimal;

public record InvestAssetRes(
        Long assetNo,
        String assetTypeCd,
        String assetCd,
        String assetNm,
        String currencyCd,
        BigDecimal currentPrice
) {
    public static InvestAssetRes of(InvestAsset a, BigDecimal price) {
        return new InvestAssetRes(a.getAssetNo(), a.getAssetTypeCd(), a.getAssetCd(),
                a.getAssetNm(), a.getCurrencyCd(), price);
    }
}
