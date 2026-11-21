package com.lucadani.backend.optimization;

/** One portfolio on the efficient frontier. */
public record FrontierPoint(double[] weights, PortfolioEvaluation evaluation) {

    public FrontierPoint {
        weights = weights.clone();
    }

    @Override
    public double[] weights() {
        return weights.clone();
    }

    public double weight(int i) {
        return weights[i];
    }
}
