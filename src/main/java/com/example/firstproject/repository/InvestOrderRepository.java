package com.example.firstproject.repository;

import com.example.firstproject.entity.InvestOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestOrderRepository extends JpaRepository<InvestOrder, Long> {

    List<InvestOrder> findByAccountNoOrderByOrderDtDesc(Long accountNo);
}
