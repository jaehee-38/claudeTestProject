package com.example.firstproject.repository;

import com.example.firstproject.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findAllByOrderByAssetTypeAscIdAsc();
}
