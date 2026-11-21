package com.lucadani.backend.web.dto;

import com.lucadani.backend.model.Reoptimization;
import com.lucadani.backend.model.ReoptimizationStatus;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * One run of a portfolio's strategy on newer prices. When it succeeded, {@code changes} compares the
 * weights held now with the new targets (both as shares of the total value, cash included) and
 * {@code turnover} is the share of the value to trade; when it failed, {@code error} says why.
 */
public record ReoptimizationResponse(
        Long id,
        ReoptimizationStatus status,
        @Nullable String error,
        @Nullable Double expectedReturn,
        @Nullable Double volatility,
        @Nullable Double sharpeRatio,
        @Nullable LocalDate statisticsFrom,
        @Nullable LocalDate statisticsTo,
        @Nullable Double turnover,
        List<WeightChange> changes,
        Instant createdAt
) {

    public record WeightChange(String symbol, double currentWeight, double targetWeight) {
    }

    public static ReoptimizationResponse of(Reoptimization reoptimization, List<WeightChange> changes) {
        return new ReoptimizationResponse(reoptimization.getId(), reoptimization.getStatus(),
                reoptimization.getError(), reoptimization.getExpectedReturn(), reoptimization.getVolatility(),
                reoptimization.getSharpeRatio(), reoptimization.getStatisticsFrom(),
                reoptimization.getStatisticsTo(), reoptimization.getTurnover(), changes,
                reoptimization.getCreatedAt());
    }
}
