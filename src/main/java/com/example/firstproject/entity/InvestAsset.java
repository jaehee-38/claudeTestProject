package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TB_ASSET")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASSET_NO")
    private Long assetNo;

    @Column(name = "ASSET_TYPE_CD", length = 20)
    private String assetTypeCd;

    @Column(name = "ASSET_CD", length = 20)
    private String assetCd;

    @Column(name = "ASSET_NM", length = 100)
    private String assetNm;

    @Column(name = "CURRENCY_CD", length = 3)
    private String currencyCd;

    @Column(name = "TRADABLE_YN", length = 1)
    private String tradableYn;
}
