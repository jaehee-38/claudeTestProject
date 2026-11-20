package com.example.firstproject.repository;

import com.example.firstproject.entity.AccountLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountLedgerRepository extends JpaRepository<AccountLedger, Long> {

    List<AccountLedger> findByAccountNoOrderByRegDtDesc(Long accountNo);
}
