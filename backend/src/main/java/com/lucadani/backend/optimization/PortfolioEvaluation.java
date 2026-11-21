package com.lucadani.backend.optimization;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.optimization.PortfolioConstraint.MaxAssets;
import com.lucadani.backend.optimization.PortfolioConstraint.MinExpectedReturn;
import com.lucadani.backend.optimization.PortfolioConstraint.SectorLimit;

import java.util.*;

/**
 * Risk/return metrics of a set of weights, and the constraints those weights break.
 *
 * @param sharpeRatio excess return per unit of volatility; 0 when the volatility is 0
 * @param violations  human-readable descriptions of broken constraints; empty when feasible
 */
public record PortfolioEvaluation(double expectedReturn,
                                  double volatility,
                                  double sharpeRatio,
                                  List<String> violations) {
    /** Numerical tolerance used when checking sums and bounds. */
    public static final double TOLERANCE = 1e-6;

    /** Weights at or below this value count as "not held". */
    public static final double HOLDING_THRESHOLD = 1e-9;

    public PortfolioEvaluation {
        violations = List.copyOf(violations);
    }

    public boolean feasible() {
        return violations.isEmpty();
    }

    public static PortfolioEvaluation evaluate(OptimizationProblem problem, double[] weights) {
        MarketStatistics stats = problem.statistics();
        int n = problem.assetCount();
        if (weights.length != n) {
            throw new IllegalArgumentException("Expected " + n + " weights, got " + weights.length);
        }

        double expectedReturn = 0;
        double variance = 0;
        for (int i = 0; i < n; i++) {
            expectedReturn += weights[i] * stats.meanReturn(i);
            for (int j = 0; j < n; j++) {
                variance += weights[i] * weights[j] * stats.covariance(i, j);
            }
        }
        double volatility = Math.sqrt(Math.max(0, variance));
        double sharpeRatio = volatility <= HOLDING_THRESHOLD ? 0 : (expectedReturn - problem.riskFreeRate()) / volatility;

        return new PortfolioEvaluation(expectedReturn, volatility, sharpeRatio, violations(problem, weights, expectedReturn));
    }

    private static List<String> violations(OptimizationProblem problem, double[] weights, double expectedReturn) {
        List<String> violations = new ArrayList<>();
        int n = problem.assetCount();

        double sum = 0;
        for (double weight : weights) {
            sum += weight;
        }
        if (Math.abs(sum - 1) > TOLERANCE) {
            violations.add(format("Weights add up to %.4f instead of 1", sum));
        }

        double[] lower = problem.lowerBounds();
        double[] upper = problem.upperBounds();
        for (int i = 0; i < n; i++) {
            if (weights[i] < lower[i] - TOLERANCE) {
                violations.add(format("%s weight %.4f is below the minimum %.4f", problem.symbol(i), weights[i], lower[i]));
            }
            if (weights[i] > upper[i] + TOLERANCE) {
                violations.add(format("%s weight %.4f is above the maximum %.4f", problem.symbol(i), weights[i], upper[i]));
            }
        }

        Map<String, Double> sectorWeights = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            sectorWeights.merge(problem.sector(i), weights[i], Double::sum);
        }
        for (SectorLimit limit : problem.constraintsOfType(SectorLimit.class)) {
            double weight = sectorWeights.getOrDefault(limit.sector(), 0.0);
            if (weight > limit.maxWeight() + TOLERANCE) {
                violations.add(format("Sector %s weight %.4f is above the maximum %.4f",
                        limit.sector(), weight, limit.maxWeight()));
            }
        }

        long held = 0;
        for (double weight : weights) {
            if (weight > HOLDING_THRESHOLD) {
                held++;
            }
        }
        for (MaxAssets limit : problem.constraintsOfType(MaxAssets.class)) {
            if (held > limit.count()) {
                violations.add(format("Portfolio holds %d assets, more than the maximum %d", held, limit.count()));
            }
        }

        for (MinExpectedReturn target : problem.constraintsOfType(MinExpectedReturn.class)) {
            if (expectedReturn < target.value() - TOLERANCE) {
                violations.add(format("Expected return %.4f is below the minimum %.4f", expectedReturn, target.value()));
            }
        }
        return violations;
    }

    private static String format(String template, Object... args) {
        return String.format(Locale.ROOT, template, args);
    }
}
