package com.lucadani.backend.optimization;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How far a portfolio is from new target weights. Weights are shares of the total value, cash included;
 * the targets hold no cash. {@code turnover} is the share of the total value that would have to change
 * hands to reach the targets: 0 means nothing to do, 1 means selling everything and buying something else.
 */
public record RebalancePlan(List<WeightChange> changes, double turnover) {

    public RebalancePlan {
        changes = List.copyOf(changes);
    }

    public record WeightChange(String symbol, double currentWeight, double targetWeight) {

        public double difference() {
            return targetWeight - currentWeight;
        }
    }

    /**
     * @param symbols       the assets the optimization chose from; {@code targetWeights[i]} is for {@code symbols.get(i)}
     * @param targetWeights new weights, summing to 1
     * @param currentValues market value of each asset held now; one missing from {@code symbols} is sold entirely
     * @param cash          uninvested cash
     */
    public static RebalancePlan of(List<String> symbols, double[] targetWeights,
                                   Map<String, Double> currentValues, double cash) {
        if (targetWeights.length != symbols.size()) {
            throw new IllegalArgumentException("Expected " + symbols.size() + " weights, got " + targetWeights.length);
        }
        if (cash < 0 || currentValues.values().stream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("Values and cash must not be negative");
        }
        double total = cash + currentValues.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            throw new IllegalArgumentException("The portfolio has no value to rebalance");
        }

        Set<String> allSymbols = new LinkedHashSet<>(symbols);
        allSymbols.addAll(currentValues.keySet());

        List<WeightChange> changes = new ArrayList<>();
        double moved = cash / total;
        for (String symbol : allSymbols) {
            int index = symbols.indexOf(symbol);
            double target = (index >= 0) ? targetWeights[index] : 0;
            double current = currentValues.getOrDefault(symbol, 0.0) / total;
            moved += Math.abs(target - current);
            // Assets neither held nor wanted are left out
            if (target > PortfolioEvaluation.HOLDING_THRESHOLD || current > 0) {
                changes.add(new WeightChange(symbol, current, target));
            }
        }
        // Every amount sold is bought back elsewhere, so the sum counts each trade twice
        return new RebalancePlan(changes, moved / 2);
    }
}
