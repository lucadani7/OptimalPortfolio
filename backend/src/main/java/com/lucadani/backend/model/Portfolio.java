package com.lucadani.backend.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A saved allocation: whole shares bought with a cash budget, following an optimization strategy. */
@Entity
@Table(name = "portfolios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Name of the strategy that produced the target weights, e.g. "max-sharpe". */
    @Column(nullable = false, length = 64)
    private String strategy;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal invested;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal leftover;

    @Embedded
    private PortfolioMetrics metrics;

    /** The constraints of the request, as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String constraintsJson;

    /** The symbols the optimization chose from, as JSON; null for portfolios saved before it was kept. */
    @JdbcTypeCode(SqlTypes.JSON)
    private @Nullable String symbolsJson;

    /** How often the portfolio is reoptimized on its own; null when it is not. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private @Nullable ReoptimizationSchedule reoptimizationSchedule;

    @Version
    private long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Holding> holdings = new ArrayList<>();

    public Portfolio(String name, String strategy, BigDecimal budget, BigDecimal invested, BigDecimal leftover,
                     PortfolioMetrics metrics, String constraintsJson, String symbolsJson) {
        rename(name);
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        if (budget.signum() <= 0) {
            throw new IllegalArgumentException("budget must be positive");
        }
        if (invested.signum() < 0 || invested.compareTo(budget) > 0) {
            throw new IllegalArgumentException("invested must be between 0 and the budget");
        }
        if (leftover.signum() < 0) {
            throw new IllegalArgumentException("leftover must not be negative");
        }
        this.budget = budget;
        this.invested = invested;
        this.leftover = leftover;
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.constraintsJson = Objects.requireNonNull(constraintsJson, "constraintsJson must not be null");
        this.symbolsJson = Objects.requireNonNull(symbolsJson, "symbolsJson must not be null");
    }

    /** Sets how often the portfolio is reoptimized on its own; null stops it. */
    public void scheduleReoptimization(@Nullable ReoptimizationSchedule schedule) {
        this.reoptimizationSchedule = schedule;
    }

    public void rename(String name) {
        Objects.requireNonNull(name, "name must not be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.name = trimmed;
    }

    public Holding addHolding(Asset asset, int shares, BigDecimal purchasePrice, LocalDate priceDate,
                              double targetWeight) {
        boolean alreadyHeld = holdings.stream().anyMatch(holding -> holding.getAsset().equals(asset));
        if (alreadyHeld) {
            throw new IllegalArgumentException(asset.getSymbol() + " is already held in this portfolio");
        }
        Holding holding = new Holding(this, asset, shares, purchasePrice, priceDate, targetWeight);
        holdings.add(holding);
        return holding;
    }

    public List<Holding> getHoldings() {
        return Collections.unmodifiableList(holdings);
    }

    /** Portfolios are identified by their database id; a new, unsaved portfolio only equals itself. */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Portfolio other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public final int hashCode() {
        return Portfolio.class.hashCode();
    }

    @Override
    public String toString() {
        return "Portfolio[" + id + ", " + name + "]";
    }
}
