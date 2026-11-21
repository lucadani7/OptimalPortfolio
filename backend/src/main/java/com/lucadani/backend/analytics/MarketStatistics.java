package com.lucadani.backend.analytics;

import java.time.LocalDate;
import java.util.List;

/**
 * Annualized return and risk statistics for a set of assets over a common period.
 * Index {@code i} in every array refers to {@code symbols().get(i)}.
 * Arrays are copied on the way in and out, so instances are immutable.
 *
 * @param observations number of daily returns the statistics are based on
 */
public record MarketStatistics(List<String> symbols,
                               LocalDate from,
                               LocalDate to,
                               int observations,
                               double[] meanReturns,
                               double[] volatilities,
                               double[][] covariance,
                               double[][] correlation) {
    public MarketStatistics {
        symbols = List.copyOf(symbols);
        meanReturns = meanReturns.clone();
        volatilities = volatilities.clone();
        covariance = deepCopy(covariance);
        correlation = deepCopy(correlation);
    }

    public int assetCount() {
        return symbols.size();
    }

    public int indexOf(String symbol) {
        int index = symbols.indexOf(symbol);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown symbol: " + symbol);
        }
        return index;
    }

    // Element accessors: fast, no copying. Use these in numeric loops.

    public double meanReturn(int i) {
        return meanReturns[i];
    }

    public double volatility(int i) {
        return volatilities[i];
    }

    public double covariance(int i, int j) {
        return covariance[i][j];
    }

    public double correlation(int i, int j) {
        return correlation[i][j];
    }

    // Whole-array accessors: return defensive copies.

    @Override
    public double[] meanReturns() {
        return meanReturns.clone();
    }

    @Override
    public double[] volatilities() {
        return volatilities.clone();
    }

    @Override
    public double[][] covariance() {
        return deepCopy(covariance);
    }

    @Override
    public double[][] correlation() {
        return deepCopy(correlation);
    }

    private static double[][] deepCopy(double[][] matrix) {
        double[][] copy = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            copy[i] = matrix[i].clone();
        }
        return copy;
    }
}
