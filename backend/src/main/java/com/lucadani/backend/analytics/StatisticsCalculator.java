package com.lucadani.backend.analytics;

import java.util.List;

/**
 * Computes return and risk statistics from historical prices.
 * <p>
 * Uses simple (arithmetic) daily returns, because the return of a portfolio is exactly
 * the weighted sum of the simple returns of its assets. Daily figures are annualized
 * by multiplying with {@code periodsPerYear}; volatilities are the square roots of the
 * annualized variances. Covariances use the sample estimator (divided by n - 1).
 */
public class StatisticsCalculator {
    public static final int TRADING_DAYS_PER_YEAR = 252;

    /** Three prices give two returns, the minimum for a sample covariance. */
    private static final int MIN_COMMON_DAYS = 3;

    private final int periodsPerYear;

    public StatisticsCalculator() {
        this(TRADING_DAYS_PER_YEAR);
    }

    public StatisticsCalculator(int periodsPerYear) {
        if (periodsPerYear <= 0) {
            throw new IllegalArgumentException("periodsPerYear must be positive");
        }
        this.periodsPerYear = periodsPerYear;
    }

    public MarketStatistics compute(List<PriceSeries> series) {
        AlignedPrices aligned = AlignedPrices.align(series);
        if (aligned.dayCount() < MIN_COMMON_DAYS) {
            throw new IllegalArgumentException("At least " + MIN_COMMON_DAYS + " common trading days are required, found " + aligned.dayCount());
        }

        double[][] returns = simpleReturns(aligned.prices());
        int assets = returns.length;

        double[] meanReturns = new double[assets];
        for (int i = 0; i < assets; i++) {
            meanReturns[i] = mean(returns[i]) * periodsPerYear;
        }

        double[][] covariance = covarianceMatrix(returns);
        double[] volatilities = new double[assets];
        for (int i = 0; i < assets; i++) {
            for (int j = 0; j < assets; j++) {
                covariance[i][j] *= periodsPerYear;
            }
            volatilities[i] = Math.sqrt(covariance[i][i]);
        }

        return new MarketStatistics(
                aligned.symbols(),
                aligned.dates().getFirst(),
                aligned.dates().getLast(),
                returns[0].length,
                meanReturns,
                volatilities,
                covariance,
                correlationMatrix(covariance)
        );
    }

    /** {@code returns[asset][t] = prices[asset][t + 1] / prices[asset][t] - 1}. */
    static double[][] simpleReturns(double[][] prices) {
        double[][] returns = new double[prices.length][];
        for (int a = 0; a < prices.length; a++) {
            double[] p = prices[a];
            double[] r = new double[p.length - 1];
            for (int t = 1; t < p.length; t++) {
                r[t - 1] = p[t] / p[t - 1] - 1;
            }
            returns[a] = r;
        }
        return returns;
    }

    static double mean(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    /** Sample covariance matrix of the rows of {@code returns}. All rows must have the same length. */
    static double[][] covarianceMatrix(double[][] returns) {
        int assets = returns.length;
        int observations = returns[0].length;

        double[] means = new double[assets];
        for (int i = 0; i < assets; i++) {
            means[i] = mean(returns[i]);
        }

        double[][] covariance = new double[assets][assets];
        for (int i = 0; i < assets; i++) {
            for (int j = i; j < assets; j++) {
                double sum = 0;
                for (int t = 0; t < observations; t++) {
                    sum += (returns[i][t] - means[i]) * (returns[j][t] - means[j]);
                }
                covariance[i][j] = sum / (observations - 1);
                covariance[j][i] = covariance[i][j];
            }
        }
        return covariance;
    }

    /**
     * Correlation matrix derived from a covariance matrix.
     * An asset with zero variance is uncorrelated with every other asset.
     */
    static double[][] correlationMatrix(double[][] covariance) {
        int assets = covariance.length;
        double[] deviations = new double[assets];
        for (int i = 0; i < assets; i++) {
            deviations[i] = Math.sqrt(covariance[i][i]);
        }

        double[][] correlation = new double[assets][assets];
        for (int i = 0; i < assets; i++) {
            for (int j = 0; j < assets; j++) {
                if (i == j) {
                    correlation[i][j] = 1.0;
                } else if (deviations[i] == 0 || deviations[j] == 0) {
                    correlation[i][j] = 0.0;
                } else {
                    double value = covariance[i][j] / (deviations[i] * deviations[j]);
                    // Guard against rounding pushing the value slightly outside [-1, 1]
                    correlation[i][j] = Math.clamp(value, -1.0, 1.0);
                }
            }
        }
        return correlation;
    }
}
