package com.lucadani.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("ETF")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Etf extends Asset {
    /** Annual expense ratio as a fraction (0.00030 = 0.03%). */
    @Column(precision = 6, scale = 5)
    private BigDecimal expenseRatio;

    public Etf(String symbol, String name, String sector, String currency, BigDecimal expenseRatio) {
        super(symbol, name, sector, currency);
        changeExpenseRatio(expenseRatio);
    }

    @Override
    public AssetType getType() {
        return AssetType.ETF;
    }

    public void changeExpenseRatio(BigDecimal expenseRatio) {
        if (expenseRatio != null && (expenseRatio.signum() < 0 || expenseRatio.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("expenseRatio must be between 0 and 1");
        }
        this.expenseRatio = expenseRatio;
    }
}
