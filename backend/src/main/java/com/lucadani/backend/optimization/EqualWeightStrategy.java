package com.lucadani.backend.optimization;

import java.util.Arrays;

/**
 * Baseline strategy: invests the same fraction in every asset.
 * It ignores returns, risk and constraints; any constraint it breaks is reported
 * by the evaluation. Useful as a reference point for the real optimizers.
 */
public final class EqualWeightStrategy implements OptimizationStrategy {
    public static final String NAME = "equal-weight";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Invests the same fraction of the budget in every asset (baseline, ignores constraints)";
    }

    @Override
    public double[] optimize(OptimizationProblem problem) {
        double[] weights = new double[problem.assetCount()];
        Arrays.fill(weights, 1.0 / weights.length);
        return weights;
    }
}
