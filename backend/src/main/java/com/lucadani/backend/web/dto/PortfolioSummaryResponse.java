package com.lucadani.backend.web.dto;

import com.lucadani.backend.model.Portfolio;

import java.math.BigDecimal;
import java.time.Instant;

/** Short description of a portfolio, for lists. */
public record PortfolioSummaryResponse(
        Long id,
        String name,
        String strategy,
        BigDecimal budget,
        BigDecimal invested,
        int holdings,
        double expectedReturn,
        double sharpeRatio,
        Instant createdAt
) {

    public static PortfolioSummaryResponse of(Portfolio portfolio) {
        return new PortfolioSummaryResponse(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getStrategy(),
                portfolio.getBudget(),
                portfolio.getInvested(),
                portfolio.getHoldings().size(),
                portfolio.getMetrics().expectedReturn(),
                portfolio.getMetrics().sharpeRatio(),
                portfolio.getCreatedAt()
        );
    }
}
