package com.lucadani.backend.optimization;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Traces the efficient frontier: for evenly spaced target returns between the minimum-variance
 * portfolio and the highest achievable return, finds the portfolio with the lowest variance.
 * Every point respects all the constraints of the problem. Points are solved in parallel.
 */
public final class EfficientFrontier {

    public static final int MIN_POINTS = 2;
    public static final int MAX_POINTS = 100;

    /**
     * @return points ordered by increasing expected return; a single point when every feasible
     * portfolio has the same expected return
     */
    public List<FrontierPoint> trace(OptimizationProblem problem, int points) {
        if (points < MIN_POINTS || points > MAX_POINTS) {
            throw new IllegalArgumentException("points must be between " + MIN_POINTS + " and " + MAX_POINTS);
        }
        double[] minVariance = MeanVarianceModel.of(problem).minimiseVariance();
        double lowReturn = PortfolioEvaluation.evaluate(problem, minVariance).expectedReturn();
        double highReturn = PortfolioEvaluation.evaluate(problem,
                MeanVarianceModel.of(problem).maximiseReturn()).expectedReturn();

        if (highReturn - lowReturn < PortfolioEvaluation.TOLERANCE) {
            return List.of(point(problem, minVariance));
        }

        double step = (highReturn - lowReturn) / (points - 1);
        return IntStream.range(0, points)
                .parallel()
                .mapToObj(k -> k == 0
                        ? point(problem, minVariance)
                        : point(problem, minimumVarianceFor(problem, lowReturn + k * step)))
                .toList();
    }

    /** The lowest-variance portfolio whose expected return is at least {@code targetReturn}. */
    double[] minimumVarianceFor(OptimizationProblem problem, double targetReturn) {
        return MeanVarianceModel.withMinReturn(problem, targetReturn).minimiseVariance();
    }

    private static FrontierPoint point(OptimizationProblem problem, double[] weights) {
        return new FrontierPoint(weights, PortfolioEvaluation.evaluate(problem, weights));
    }
}
