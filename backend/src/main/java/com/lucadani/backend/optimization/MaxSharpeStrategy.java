package com.lucadani.backend.optimization;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Portfolio with the highest Sharpe ratio on the efficient frontier (the "tangency" portfolio).
 * <p>
 * A coarse scan of the frontier finds the best region; a golden-section search between the
 * neighboring points then refines the target return. Along the frontier the Sharpe ratio rises
 * and then falls, so the search converges to the maximum.
 */
public final class MaxSharpeStrategy implements OptimizationStrategy {

    public static final String NAME = "max-sharpe";

    private static final int SCAN_POINTS = 21;
    private static final int REFINEMENT_STEPS = 30;
    private static final double GOLDEN_RATIO = (Math.sqrt(5) - 1) / 2;

    private final EfficientFrontier frontier;

    public MaxSharpeStrategy(EfficientFrontier frontier) {
        this.frontier = frontier;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Highest return per unit of risk on the efficient frontier (Markowitz tangency portfolio)";
    }

    @Override
    public double[] optimize(OptimizationProblem problem) {
        List<FrontierPoint> scan = frontier.trace(problem, SCAN_POINTS);
        int best = 0;
        for (int k = 1; k < scan.size(); k++) {
            if (scan.get(k).evaluation().sharpeRatio() > scan.get(best).evaluation().sharpeRatio()) {
                best = k;
            }
        }
        if (scan.size() < 3) {
            return scan.get(best).weights();
        }

        double low = scan.get(Math.max(0, best - 1)).evaluation().expectedReturn();
        double high = scan.get(Math.min(scan.size() - 1, best + 1)).evaluation().expectedReturn();
        FrontierPoint refined = refine(problem, low, high);

        return Stream.of(scan.get(best), refined)
                .max(Comparator.comparingDouble(point -> point.evaluation().sharpeRatio()))
                .orElseThrow()
                .weights();
    }

    /** Golden-section search for the target return with the highest Sharpe ratio in [low, high]. */
    private FrontierPoint refine(OptimizationProblem problem, double low, double high) {
        double a = low;
        double b = high;
        double c = b - GOLDEN_RATIO * (b - a);
        double d = a + GOLDEN_RATIO * (b - a);
        FrontierPoint pc = solve(problem, c);
        FrontierPoint pd = solve(problem, d);

        for (int step = 0; step < REFINEMENT_STEPS && b - a > PortfolioEvaluation.TOLERANCE; step++) {
            if (pc.evaluation().sharpeRatio() >= pd.evaluation().sharpeRatio()) {
                b = d;
                d = c;
                pd = pc;
                c = b - GOLDEN_RATIO * (b - a);
                pc = solve(problem, c);
            } else {
                a = c;
                c = d;
                pc = pd;
                d = a + GOLDEN_RATIO * (b - a);
                pd = solve(problem, d);
            }
        }
        return pc.evaluation().sharpeRatio() >= pd.evaluation().sharpeRatio() ? pc : pd;
    }

    private FrontierPoint solve(OptimizationProblem problem, double targetReturn) {
        double[] weights = frontier.minimumVarianceFor(problem, targetReturn);
        return new FrontierPoint(weights, PortfolioEvaluation.evaluate(problem, weights));
    }
}
