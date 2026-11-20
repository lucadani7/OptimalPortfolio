package com.lucadani.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@DiscriminatorValue("BOND")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bond extends Asset {
    /** Annual coupon rate as a fraction (0.04500 = 4.5%). Null for zero-coupon bonds. */
    @Column(precision = 7, scale = 5)
    private BigDecimal couponRate;

    private LocalDate maturityDate;

    public Bond(String symbol, String name, String sector, String currency,
                BigDecimal couponRate, LocalDate maturityDate) {
        super(symbol, name, sector, currency);
        updateTerms(couponRate, maturityDate);
    }

    @Override
    public AssetType getType() {
        return AssetType.BOND;
    }

    public void updateTerms(BigDecimal couponRate, LocalDate maturityDate) {
        if (couponRate != null && (couponRate.signum() < 0 || couponRate.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("couponRate must be between 0 and 1");
        }
        this.couponRate = couponRate;
        this.maturityDate = Objects.requireNonNull(maturityDate, "maturityDate must not be null");
    }
}
