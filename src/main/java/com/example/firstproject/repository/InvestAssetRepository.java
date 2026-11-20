package com.example.firstproject.repository;

import com.example.firstproject.entity.InvestAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface InvestAssetRepository extends JpaRepository<InvestAsset, Long> {

    @Query(value = "SELECT cur_prc FROM TB_ASSET_PRICE_REALTIME WHERE asset_no = :assetNo", nativeQuery = true)
    Optional<BigDecimal> findRealtimePrice(@Param("assetNo") Long assetNo);

    @Query(value = "SELECT close_prc FROM TB_ASSET_PRICE_DAILY WHERE asset_no = :assetNo ORDER BY base_de DESC LIMIT 1", nativeQuery = true)
    Optional<BigDecimal> findLatestDailyPrice(@Param("assetNo") Long assetNo);
}
