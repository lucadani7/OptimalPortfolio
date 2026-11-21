package com.lucadani.backend.optimization;

/** An algorithm that chooses portfolio weights for an {@link OptimizationProblem}. */
public interface OptimizationStrategy {
    /** Stable identifier used in the API, e.g. {@code "equal-weight"}. */
    String name();

    /** One-sentence explanation shown to users. */
    String description();

    /**
     * Chooses the weights of the portfolio.
     *
     * @return one weight per asset, aligned with {@code problem.statistics().symbols()}
     */
    double[] optimize(OptimizationProblem problem);
}
