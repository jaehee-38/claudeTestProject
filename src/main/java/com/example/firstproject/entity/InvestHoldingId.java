package com.example.firstproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InvestHoldingId implements Serializable {

    @Column(name = "ACCOUNT_NO")
    private Long accountNo;

    @Column(name = "ASSET_NO")
    private Long assetNo;
}
