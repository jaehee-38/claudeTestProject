package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_INVEST_ACCOUNT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ACCOUNT_NO")
    private Long accountNo;

    @Column(name = "MEMBER_NO")
    private Long memberNo;

    @Column(name = "ACCOUNT_NM", length = 50)
    private String accountNm;

    @Column(name = "DEPOSIT_AMT", precision = 18, scale = 0)
    private BigDecimal depositAmt;

    @Column(name = "ORDER_HOLD_AMT", precision = 18, scale = 0)
    private BigDecimal orderHoldAmt;

    @Column(name = "INIT_AMT", precision = 18, scale = 0)
    private BigDecimal initAmt;

    @Column(name = "ACCOUNT_STAT_CD", length = 20)
    private String accountStatCd;

    @Version
    @Column(name = "VERSION")
    private Integer version;

    @Column(name = "REG_ID", length = 50)
    private String regId;

    @Column(name = "REG_DT")
    private LocalDateTime regDt;

    @Column(name = "MOD_DT")
    private LocalDateTime modDt;

    public static InvestAccount open(Long memberNo, String accountNm, BigDecimal initAmt, String regId) {
        InvestAccount a = new InvestAccount();
        a.memberNo = memberNo;
        a.accountNm = accountNm;
        a.depositAmt = initAmt;
        a.orderHoldAmt = BigDecimal.ZERO;
        a.initAmt = initAmt;
        a.accountStatCd = "ACTIVE";
        a.regId = regId;
        a.regDt = LocalDateTime.now();
        return a;
    }

    public void deductDeposit(BigDecimal amount) {
        this.depositAmt = this.depositAmt.subtract(amount);
        this.modDt = LocalDateTime.now();
    }

    public void addDeposit(BigDecimal amount) {
        this.depositAmt = this.depositAmt.add(amount);
        this.modDt = LocalDateTime.now();
    }
}
