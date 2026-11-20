package com.example.firstproject.repository;

import com.example.firstproject.entity.InvestExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestExecutionRepository extends JpaRepository<InvestExecution, Long> {

    List<InvestExecution> findByOrderNo(Long orderNo);
}
