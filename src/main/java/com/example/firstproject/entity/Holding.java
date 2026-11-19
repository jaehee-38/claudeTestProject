package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 내가 직접 등록한 보유 자산 1건.
 */
@Entity
@Table(name = "holding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(nullable = false, length = 100)
    private String name;

    /** 종목코드 / 통화코드. 금·부동산·기타는 비워둠 */
    @Column(length = 30)
    private String code;

    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal quantity;

    /** 1단위당 평균 매입가 (원) */
    @Column(name = "average_price", nullable = false, precision = 24, scale = 4)
    private BigDecimal averagePrice;

    /** 1단위당 현재가를 직접 입력 (부동산·기타, 또는 시세 API 대신 쓰고 싶을 때) */
    @Column(name = "manual_price", precision = 24, scale = 4)
    private BigDecimal manualPrice;

    @Column(length = 255)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Holding(AssetType assetType, String name, String code, BigDecimal quantity,
                   BigDecimal averagePrice, BigDecimal manualPrice, String memo) {
        update(assetType, name, code, quantity, averagePrice, manualPrice, memo);
    }

    public void update(AssetType assetType, String name, String code, BigDecimal quantity,
                       BigDecimal averagePrice, BigDecimal manualPrice, String memo) {
        this.assetType = assetType;
        this.name = name;
        this.code = code;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.manualPrice = manualPrice;
        this.memo = memo;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
