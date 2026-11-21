package com.lucadani.backend.web.dto;

import com.lucadani.backend.model.Holding;
import com.lucadani.backend.model.Portfolio;
import com.lucadani.backend.model.PortfolioMetrics;
import com.lucadani.backend.repository.PriceRow;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * A saved portfolio with its holdings and its value at the latest known prices.
 * The risk/return figures are those computed when the portfolio was created.
 */
public record PortfolioResponse(
        Long id,
        String name,
        String strategy,
        BigDecimal budget,
        BigDecimal invested,
        BigDecimal leftover,
        double expectedReturn,
        double volatility,
        double sharpeRatio,
        double riskFreeRate,
        LocalDate statisticsFrom,
        LocalDate statisticsTo,
        List<ConstraintRequest> constraints,
        List<HoldingResponse> holdings,
        Valuation valuation,
        Instant createdAt,
        Instant updatedAt
) {

    /** {@code current*} fields are null when no price is known for the asset. */
    public record HoldingResponse(
            String symbol,
            String name,
            int shares,
            BigDecimal purchasePrice,
            LocalDate priceDate,
            double targetWeight,
            BigDecimal cost,
            @Nullable BigDecimal currentPrice,
            @Nullable LocalDate currentPriceDate,
            @Nullable BigDecimal currentValue
    ) {
    }

    /**
     * Market value of the holdings at their latest prices plus the uninvested cash.
     * Holdings without a known price are counted at cost.
     *
     * @param profitLoss change in value of the holdings since purchase (cash does not change)
     * @param returnRate profit or loss as a fraction of the budget
     */
    public record Valuation(BigDecimal currentValue, BigDecimal profitLoss, double returnRate) {
    }

    public static PortfolioResponse of(Portfolio portfolio, List<ConstraintRequest> constraints,
                                       Map<String, PriceRow> latestPrices) {
        List<HoldingResponse> holdings = portfolio.getHoldings().stream()
                .map(holding -> holding(holding, latestPrices.get(holding.getAsset().getSymbol())))
                .toList();

        BigDecimal currentValue = portfolio.getLeftover();
        BigDecimal profitLoss = BigDecimal.ZERO;
        for (HoldingResponse holding : holdings) {
            BigDecimal value = (holding.currentValue() != null) ? holding.currentValue() : holding.cost();
            currentValue = currentValue.add(value);
            profitLoss = profitLoss.add(value.subtract(holding.cost()));
        }
        double returnRate = profitLoss.doubleValue() / portfolio.getBudget().doubleValue();

        PortfolioMetrics metrics = portfolio.getMetrics();
        return new PortfolioResponse(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getStrategy(),
                portfolio.getBudget(),
                portfolio.getInvested(),
                portfolio.getLeftover(),
                metrics.expectedReturn(),
                metrics.volatility(),
                metrics.sharpeRatio(),
                metrics.riskFreeRate(),
                metrics.statisticsFrom(),
                metrics.statisticsTo(),
                constraints,
                holdings,
                new Valuation(currentValue, profitLoss, returnRate),
                portfolio.getCreatedAt(),
                portfolio.getUpdatedAt()
        );
    }

    private static HoldingResponse holding(Holding holding, @Nullable PriceRow latest) {
        BigDecimal currentValue = (latest == null) ? null
                : latest.close().multiply(BigDecimal.valueOf(holding.getShares())).setScale(2, RoundingMode.HALF_UP);
        return new HoldingResponse(
                holding.getAsset().getSymbol(),
                holding.getAsset().getName(),
                holding.getShares(),
                holding.getPurchasePrice(),
                holding.getPriceDate(),
                holding.getTargetWeight(),
                holding.cost(),
                (latest == null) ? null : latest.close(),
                (latest == null) ? null : latest.tradeDate(),
                currentValue
        );
    }
}
