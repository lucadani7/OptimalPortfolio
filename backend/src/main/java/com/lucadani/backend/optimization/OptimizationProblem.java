package com.lucadani.backend.optimization;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.optimization.PortfolioConstraint.AssetWeightBounds;
import com.lucadani.backend.optimization.PortfolioConstraint.WeightBounds;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Everything a strategy needs to build a portfolio: the statistics of the candidate assets,
 * their sectors, the constraints and the risk-free rate used for the Sharpe ratio.
 * Asset {@code i} is {@code statistics().symbols().get(i)} everywhere.
 */
public record OptimizationProblem(MarketStatistics statistics,
                                  Map<String, String> sectors,
                                  List<PortfolioConstraint> constraints,
                                  double riskFreeRate) {
    public OptimizationProblem {
        Objects.requireNonNull(statistics, "statistics must not be null");
        sectors = Map.copyOf(sectors);
        constraints = List.copyOf(constraints);

        for (String symbol : statistics.symbols()) {
            if (!sectors.containsKey(symbol)) {
                throw new IllegalArgumentException("Missing sector for " + symbol);
            }
        }
        for (PortfolioConstraint constraint : constraints) {
            if (constraint instanceof AssetWeightBounds bounds && !statistics.symbols().contains(bounds.symbol())) {
                throw new IllegalArgumentException("Weight bounds refer to an asset that is not in the problem: "
                        + bounds.symbol());
            }
        }
        if (!Double.isFinite(riskFreeRate)) {
            throw new IllegalArgumentException("riskFreeRate must be a finite number");
        }

        double[][] bounds = weightBounds(statistics, constraints);
        double lowerSum = 0;
        double upperSum = 0;
        for (int i = 0; i < statistics.assetCount(); i++) {
            if (bounds[0][i] > bounds[1][i]) {
                throw new IllegalArgumentException("Weight bounds of " + statistics.symbols().get(i) + " are contradictory");
            }
            lowerSum += bounds[0][i];
            upperSum += bounds[1][i];
        }
        if (lowerSum > 1 + PortfolioEvaluation.TOLERANCE) {
            throw new IllegalArgumentException("Minimum weights add up to more than 100%");
        }
        if (upperSum < 1 - PortfolioEvaluation.TOLERANCE) {
            throw new IllegalArgumentException("Maximum weights add up to less than 100%");
        }
    }

    public int assetCount() {
        return statistics.assetCount();
    }

    public String symbol(int i) {
        return statistics.symbols().get(i);
    }

    public String sector(int i) {
        return sectors.get(symbol(i));
    }

    /** Effective minimum weight of every asset, after combining all bound constraints. */
    public double[] lowerBounds() {
        return weightBounds(statistics, constraints)[0];
    }

    /** Effective maximum weight of every asset, after combining all bound constraints. */
    public double[] upperBounds() {
        return weightBounds(statistics, constraints)[1];
    }

    /** All constraints of the given kind, e.g. {@code constraintsOfType(SectorLimit.class)}. */
    public <T extends PortfolioConstraint> List<T> constraintsOfType(Class<T> type) {
        return constraints.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** Intersects every bound constraint, starting from [0, 1]. Returns {lower[], upper[]}. */
    private static double[][] weightBounds(MarketStatistics statistics, List<PortfolioConstraint> constraints) {
        int n = statistics.assetCount();
        double[] lower = new double[n];
        double[] upper = new double[n];
        Arrays.fill(upper, 1.0);

        for (PortfolioConstraint constraint : constraints) {
            switch (constraint) {
                case WeightBounds b -> {
                    for (int i = 0; i < n; i++) {
                        lower[i] = Math.max(lower[i], b.min());
                        upper[i] = Math.min(upper[i], b.max());
                    }
                }
                case AssetWeightBounds b -> {
                    int i = statistics.indexOf(b.symbol());
                    lower[i] = Math.max(lower[i], b.min());
                    upper[i] = Math.min(upper[i], b.max());
                }
                default -> {
                    // Not a bound constraint
                }
            }
        }
        return new double[][]{lower, upper};
    }
}
