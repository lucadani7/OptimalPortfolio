package com.lucadani.backend.optimization;

/**
 * Markowitz minimum-variance portfolio: the least risky portfolio that satisfies every constraint.
 * With a {@link PortfolioConstraint.MinExpectedReturn} constraint this is the classic
 * "lowest risk for a target return" problem.
 */
public final class MinimumVarianceStrategy implements OptimizationStrategy {

    public static final String NAME = "min-variance";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Lowest-risk portfolio that satisfies every constraint (Markowitz)";
    }

    @Override
    public double[] optimize(OptimizationProblem problem) {
        return MeanVarianceModel.of(problem).minimiseVariance();
    }
}
