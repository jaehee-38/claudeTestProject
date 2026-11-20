package com.example.firstproject.dto;

import com.example.firstproject.entity.InvestAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminInvestAccountRes(
        Long accountNo,
        Long memberNo,
        String loginId,
        String accountNm,
        BigDecimal depositAmt,
        BigDecimal initAmt,
        String accountStatCd,
        LocalDateTime regDt
) {
    public static AdminInvestAccountRes of(InvestAccount a, String loginId) {
        return new AdminInvestAccountRes(
                a.getAccountNo(), a.getMemberNo(), loginId,
                a.getAccountNm(), a.getDepositAmt(), a.getInitAmt(),
                a.getAccountStatCd(), a.getRegDt()
        );
    }
}
