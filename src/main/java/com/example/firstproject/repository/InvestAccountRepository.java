package com.example.firstproject.repository;

import com.example.firstproject.entity.InvestAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestAccountRepository extends JpaRepository<InvestAccount, Long> {

    List<InvestAccount> findByMemberNo(Long memberNo);
}
