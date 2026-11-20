package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_HOLDING")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestHolding {

    @EmbeddedId
    private InvestHoldingId id;

    @Column(name = "HOLD_QTY", precision = 18, scale = 6)
    private BigDecimal holdQty;

    @Column(name = "SELLABLE_QTY", precision = 18, scale = 6)
    private BigDecimal sellableQty;

    @Column(name = "AVG_BUY_PRC", precision = 18, scale = 4)
    private BigDecimal avgBuyPrc;

    @Column(name = "TOT_BUY_AMT", precision = 18, scale = 0)
    private BigDecimal totBuyAmt;

    @Version
    @Column(name = "VERSION")
    private Integer version;

    @Column(name = "MOD_DT")
    private LocalDateTime modDt;

    public static InvestHolding create(Long accountNo, Long assetNo, BigDecimal qty, BigDecimal prc, BigDecimal amt) {
        InvestHolding h = new InvestHolding();
        h.id = new InvestHoldingId(accountNo, assetNo);
        h.holdQty = qty;
        h.sellableQty = qty;
        h.avgBuyPrc = prc;
        h.totBuyAmt = amt;
        h.modDt = LocalDateTime.now();
        return h;
    }

    public void addBuy(BigDecimal qty, BigDecimal amt) {
        BigDecimal newTotAmt = this.totBuyAmt.add(amt);
        BigDecimal newTotQty = this.holdQty.add(qty);
        this.holdQty = newTotQty;
        this.sellableQty = this.sellableQty.add(qty);
        this.totBuyAmt = newTotAmt;
        this.avgBuyPrc = newTotAmt.divide(newTotQty, 4, RoundingMode.HALF_UP);
        this.modDt = LocalDateTime.now();
    }

    public void reduceSell(BigDecimal qty) {
        BigDecimal soldRatio = qty.divide(this.holdQty, 10, RoundingMode.HALF_UP);
        this.totBuyAmt = this.totBuyAmt.subtract(this.totBuyAmt.multiply(soldRatio).setScale(0, RoundingMode.DOWN));
        this.holdQty = this.holdQty.subtract(qty);
        this.sellableQty = this.sellableQty.subtract(qty);
        this.modDt = LocalDateTime.now();
    }
}
