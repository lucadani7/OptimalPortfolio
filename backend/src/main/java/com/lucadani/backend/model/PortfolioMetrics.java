package com.lucadani.backend.model;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;

/**
 * Risk/return figures of a portfolio, as computed when it was created,
 * and the period of historical prices they were based on.
 */
@Embeddable
public record PortfolioMetrics(
        double expectedReturn,
        double volatility,
        double sharpeRatio,
        double riskFreeRate,
        LocalDate statisticsFrom,
        LocalDate statisticsTo
) {
}
