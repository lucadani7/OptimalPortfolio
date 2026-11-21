package com.lucadani.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One run of a portfolio's strategy on newer prices. A successful run records the new figures and
 * how far the portfolio is from the new weights; a failed one records why. Never changed once saved.
 */
@Entity
@Table(name = "reoptimizations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reoptimization {

    public static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long portfolioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReoptimizationStatus status;

    @Column(length = MAX_ERROR_LENGTH)
    private @Nullable String error;

    private @Nullable Double expectedReturn;

    private @Nullable Double volatility;

    private @Nullable Double sharpeRatio;

    private @Nullable LocalDate statisticsFrom;

    private @Nullable LocalDate statisticsTo;

    private @Nullable Double turnover;

    /** The weight changes, as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    private @Nullable String changesJson;

    @Column(nullable = false)
    private Instant createdAt;

    public static Reoptimization succeeded(Long portfolioId, double expectedReturn, double volatility,
                                           double sharpeRatio, LocalDate statisticsFrom, LocalDate statisticsTo,
                                           double turnover, String changesJson, Instant now) {
        Reoptimization reoptimization = create(portfolioId, ReoptimizationStatus.SUCCEEDED, now);
        reoptimization.expectedReturn = expectedReturn;
        reoptimization.volatility = volatility;
        reoptimization.sharpeRatio = sharpeRatio;
        reoptimization.statisticsFrom = Objects.requireNonNull(statisticsFrom);
        reoptimization.statisticsTo = Objects.requireNonNull(statisticsTo);
        reoptimization.turnover = turnover;
        reoptimization.changesJson = Objects.requireNonNull(changesJson);
        return reoptimization;
    }

    public static Reoptimization failed(Long portfolioId, String error, Instant now) {
        Reoptimization reoptimization = create(portfolioId, ReoptimizationStatus.FAILED, now);
        reoptimization.error = (error.length() > MAX_ERROR_LENGTH) ? error.substring(0, MAX_ERROR_LENGTH) : error;
        return reoptimization;
    }

    private static Reoptimization create(Long portfolioId, ReoptimizationStatus status, Instant now) {
        Reoptimization reoptimization = new Reoptimization();
        reoptimization.portfolioId = Objects.requireNonNull(portfolioId);
        reoptimization.status = status;
        reoptimization.createdAt = now;
        return reoptimization;
    }
}
