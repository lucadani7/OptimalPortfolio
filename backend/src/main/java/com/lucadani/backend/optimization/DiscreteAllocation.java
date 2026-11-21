package com.lucadani.backend.optimization;

import java.time.Duration;
import java.util.List;

/**
 * Whole numbers of shares to buy with a cash budget, chosen to match target weights as closely as possible.
 * Index {@code i} refers to {@code symbols.get(i)} in every array.
 *
 * @param shares        number of shares to buy of each asset
 * @param prices        price per share used for the allocation
 * @param targetWeights weights the allocation tries to match
 * @param actualWeights weights actually obtained, as fractions of the budget
 * @param invested      money spent on shares
 * @param leftover      cash that is not invested
 * @param optimal       true if the solver proved the allocation optimal, false if it stopped at the time limit
 */
public record DiscreteAllocation(List<String> symbols,
                                int[] shares,
                                double[] prices,
                                double[] targetWeights,
                                double[] actualWeights,
                                double budget,
                                double invested,
                                double leftover,
                                boolean optimal,
                                Duration solveTime) {

    public DiscreteAllocation {
        symbols = List.copyOf(symbols);
        shares = shares.clone();
        prices = prices.clone();
        targetWeights = targetWeights.clone();
        actualWeights = actualWeights.clone();
    }

    public int shares(int i) {
        return shares[i];
    }

    public double price(int i) {
        return prices[i];
    }

    public double targetWeight(int i) {
        return targetWeights[i];
    }

    public double actualWeight(int i) {
        return actualWeights[i];
    }

    @Override
    public int[] shares() {
        return shares.clone();
    }

    @Override
    public double[] prices() {
        return prices.clone();
    }

    @Override
    public double[] targetWeights() {
        return targetWeights.clone();
    }

    @Override
    public double[] actualWeights() {
        return actualWeights.clone();
    }
}
