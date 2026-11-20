package com.example.firstproject.dto;

import com.example.firstproject.entity.InvestOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvestOrderRes(
        Long orderNo,
        Long accountNo,
        Long assetNo,
        String orderSideCd,
        String orderTypeCd,
        BigDecimal orderQty,
        BigDecimal orderPrc,
        BigDecimal execQty,
        String orderStatCd,
        String rejectReason,
        LocalDateTime orderDt
) {
    public static InvestOrderRes from(InvestOrder o) {
        return new InvestOrderRes(
                o.getOrderNo(), o.getAccountNo(), o.getAssetNo(),
                o.getOrderSideCd(), o.getOrderTypeCd(),
                o.getOrderQty(), o.getOrderPrc(), o.getExecQty(),
                o.getOrderStatCd(), o.getRejectReason(), o.getOrderDt()
        );
    }
}
