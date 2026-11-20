package com.example.firstproject.dto;

import com.example.firstproject.entity.InvestAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvestAccountRes(
        Long accountNo,
        Long memberNo,
        String accountNm,
        BigDecimal depositAmt,
        BigDecimal initAmt,
        String accountStatCd,
        LocalDateTime regDt
) {
    public static InvestAccountRes from(InvestAccount a) {
        return new InvestAccountRes(
                a.getAccountNo(), a.getMemberNo(), a.getAccountNm(),
                a.getDepositAmt(), a.getInitAmt(), a.getAccountStatCd(), a.getRegDt()
        );
    }
}
