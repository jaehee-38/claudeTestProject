package com.example.firstproject.dto;

import com.example.firstproject.entity.AssetType;
import com.example.firstproject.entity.Holding;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 보유 자산 원본 데이터 (시세 평가 없이 DB 값 그대로). 관리자 화면용 */
public record HoldingDto(Long id, AssetType assetType, String assetTypeLabel, String name, String code,
                         BigDecimal quantity, BigDecimal averagePrice, BigDecimal manualPrice, String memo,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static HoldingDto from(Holding h) {
        return new HoldingDto(h.getId(), h.getAssetType(), h.getAssetType().getLabel(), h.getName(), h.getCode(),
                h.getQuantity().stripTrailingZeros(), h.getAveragePrice().stripTrailingZeros(),
                h.getManualPrice() == null ? null : h.getManualPrice().stripTrailingZeros(),
                h.getMemo(), h.getCreatedAt(), h.getUpdatedAt());
    }
}
