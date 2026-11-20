package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ACCOUNT_LEDGER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LEDGER_NO")
    private Long ledgerNo;

    @Column(name = "ACCOUNT_NO")
    private Long accountNo;

    @Column(name = "TXN_TYPE_CD", length = 20)
    private String txnTypeCd; // BUY / SELL / DEPOSIT / WITHDRAW

    @Column(name = "TXN_AMT", precision = 18, scale = 0)
    private BigDecimal txnAmt;

    @Column(name = "BALANCE_AFTER", precision = 18, scale = 0)
    private BigDecimal balanceAfter;

    @Column(name = "REF_TYPE_CD", length = 20)
    private String refTypeCd;

    @Column(name = "REF_NO")
    private Long refNo;

    @Column(name = "REMARK", length = 200)
    private String remark;

    @Column(name = "REG_DT")
    private LocalDateTime regDt;

    public static AccountLedger of(Long accountNo, String txnType, BigDecimal txnAmt,
                                    BigDecimal balanceAfter, String refType, Long refNo, String remark) {
        AccountLedger l = new AccountLedger();
        l.accountNo = accountNo;
        l.txnTypeCd = txnType;
        l.txnAmt = txnAmt;
        l.balanceAfter = balanceAfter;
        l.refTypeCd = refType;
        l.refNo = refNo;
        l.remark = remark;
        l.regDt = LocalDateTime.now();
        return l;
    }
}
