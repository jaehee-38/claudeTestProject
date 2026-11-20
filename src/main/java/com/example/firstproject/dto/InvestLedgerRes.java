package com.example.firstproject.dto;

import com.example.firstproject.entity.AccountLedger;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvestLedgerRes(
        Long ledgerNo,
        String txnTypeCd,
        BigDecimal txnAmt,
        BigDecimal balanceAfter,
        String refTypeCd,
        Long refNo,
        String remark,
        LocalDateTime regDt
) {
    public static InvestLedgerRes from(AccountLedger l) {
        return new InvestLedgerRes(
                l.getLedgerNo(), l.getTxnTypeCd(), l.getTxnAmt(),
                l.getBalanceAfter(), l.getRefTypeCd(), l.getRefNo(),
                l.getRemark(), l.getRegDt()
        );
    }
}
