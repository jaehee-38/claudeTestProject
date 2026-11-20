package com.example.firstproject.repository;

import com.example.firstproject.entity.InvestHolding;
import com.example.firstproject.entity.InvestHoldingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestHoldingRepository extends JpaRepository<InvestHolding, InvestHoldingId> {

    List<InvestHolding> findByIdAccountNo(Long accountNo);
}
