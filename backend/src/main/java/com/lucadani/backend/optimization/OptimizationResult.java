package com.lucadani.backend.optimization;

import java.time.Duration;
import java.util.List;

/**
 * Weights chosen by a strategy, with their evaluation.
 * {@code weights[i]} is the weight of {@code symbols.get(i)}.
 */
public record OptimizationResult(String strategy,
                                 List<String> symbols,
                                 double[] weights,
                                 PortfolioEvaluation evaluation,
                                 Duration solveTime) {
    public OptimizationResult {
        symbols = List.copyOf(symbols);
        weights = weights.clone();
        if (weights.length != symbols.size()) {
            throw new IllegalArgumentException("Expected " + symbols.size() + " weights, got " + weights.length);
        }
    }

    public static OptimizationResult of(String strategy, OptimizationProblem problem,
                                        double[] weights, Duration solveTime) {
        return new OptimizationResult(strategy, problem.statistics().symbols(), weights,
                PortfolioEvaluation.evaluate(problem, weights), solveTime);
    }

    public double weight(int i) {
        return weights[i];
    }

    @Override
    public double[] weights() {
        return weights.clone();
    }
}
