package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_EXECUTION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXEC_NO")
    private Long execNo;

    @Column(name = "ORDER_NO")
    private Long orderNo;

    @Column(name = "EXEC_QTY", precision = 18, scale = 6)
    private BigDecimal execQty;

    @Column(name = "EXEC_PRC", precision = 18, scale = 4)
    private BigDecimal execPrc;

    @Column(name = "EXEC_AMT", precision = 18, scale = 0)
    private BigDecimal execAmt;

    @Column(name = "FEE_AMT", precision = 18, scale = 0)
    private BigDecimal feeAmt;

    @Column(name = "TAX_AMT", precision = 18, scale = 0)
    private BigDecimal taxAmt;

    @Column(name = "EXEC_DT")
    private LocalDateTime execDt;

    public static InvestExecution of(Long orderNo, BigDecimal qty, BigDecimal prc,
                                      BigDecimal amt, BigDecimal fee, BigDecimal tax) {
        InvestExecution e = new InvestExecution();
        e.orderNo = orderNo;
        e.execQty = qty;
        e.execPrc = prc;
        e.execAmt = amt;
        e.feeAmt = fee;
        e.taxAmt = tax;
        e.execDt = LocalDateTime.now();
        return e;
    }
}
