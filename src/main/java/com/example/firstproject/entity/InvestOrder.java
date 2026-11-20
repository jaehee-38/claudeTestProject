package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_ORDER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_NO")
    private Long orderNo;

    @Column(name = "ACCOUNT_NO")
    private Long accountNo;

    @Column(name = "ASSET_NO")
    private Long assetNo;

    @Column(name = "ORDER_SIDE_CD", length = 10)
    private String orderSideCd; // BUY / SELL

    @Column(name = "ORDER_TYPE_CD", length = 10)
    private String orderTypeCd; // MARKET / LIMIT

    @Column(name = "ORDER_QTY", precision = 18, scale = 6)
    private BigDecimal orderQty;

    @Column(name = "ORDER_PRC", precision = 18, scale = 4)
    private BigDecimal orderPrc;

    @Column(name = "EXEC_QTY", precision = 18, scale = 6)
    private BigDecimal execQty;

    @Column(name = "ORDER_STAT_CD", length = 20)
    private String orderStatCd; // PENDING / FILLED / REJECTED

    @Column(name = "REJECT_REASON", length = 200)
    private String rejectReason;

    @Column(name = "ORDER_DT")
    private LocalDateTime orderDt;

    @Column(name = "REG_ID", length = 50)
    private String regId;

    @Column(name = "REG_DT")
    private LocalDateTime regDt;

    @Column(name = "MOD_DT")
    private LocalDateTime modDt;

    public static InvestOrder create(Long accountNo, Long assetNo, String side, BigDecimal qty, BigDecimal prc, String regId) {
        InvestOrder o = new InvestOrder();
        o.accountNo = accountNo;
        o.assetNo = assetNo;
        o.orderSideCd = side;
        o.orderTypeCd = "MARKET";
        o.orderQty = qty;
        o.orderPrc = prc;
        o.execQty = BigDecimal.ZERO;
        o.orderStatCd = "PENDING";
        o.orderDt = LocalDateTime.now();
        o.regId = regId;
        o.regDt = LocalDateTime.now();
        return o;
    }

    public void fill(BigDecimal execQty) {
        this.execQty = execQty;
        this.orderStatCd = "FILLED";
        this.modDt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.orderStatCd = "REJECTED";
        this.rejectReason = reason;
        this.modDt = LocalDateTime.now();
    }
}
