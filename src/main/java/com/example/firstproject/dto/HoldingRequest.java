package com.example.firstproject.dto;

import com.example.firstproject.entity.AssetType;

import java.math.BigDecimal;

public record HoldingRequest(
        AssetType assetType,
        String name,
        String code,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal manualPrice,
        String memo
) {}
