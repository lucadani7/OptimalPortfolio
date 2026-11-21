package com.lucadani.backend.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/** Whole shares of one asset bought for a portfolio. Created only through {@link Portfolio#addHolding}. */
@Entity
@Table(name = "holdings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    private int shares;

    /** Price per share at which the allocation was computed. */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal purchasePrice;

    /** Trading day of {@link #purchasePrice}. */
    @Column(nullable = false)
    private LocalDate priceDate;

    /** Weight the optimization targeted for this asset. */
    @Column(nullable = false)
    private double targetWeight;

    Holding(Portfolio portfolio, Asset asset, int shares, BigDecimal purchasePrice, LocalDate priceDate,
            double targetWeight) {
        if (shares <= 0) {
            throw new IllegalArgumentException("shares must be positive");
        }
        if (purchasePrice.signum() <= 0) {
            throw new IllegalArgumentException("purchasePrice must be positive");
        }
        if (!(targetWeight >= 0 && targetWeight <= 1)) {
            throw new IllegalArgumentException("targetWeight must be between 0 and 1");
        }
        this.portfolio = Objects.requireNonNull(portfolio, "portfolio must not be null");
        this.asset = Objects.requireNonNull(asset, "asset must not be null");
        this.shares = shares;
        this.purchasePrice = purchasePrice;
        this.priceDate = Objects.requireNonNull(priceDate, "priceDate must not be null");
        this.targetWeight = targetWeight;
    }

    /** Money spent on this holding, rounded to cents. */
    public BigDecimal cost() {
        return purchasePrice.multiply(BigDecimal.valueOf(shares)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Holding other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public final int hashCode() {
        return Holding.class.hashCode();
    }

    @Override
    public String toString() {
        return "Holding[" + id + ", " + shares + " shares]";
    }
}
