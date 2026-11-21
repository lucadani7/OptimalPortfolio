package com.lucadani.backend.optimization;

import com.lucadani.backend.exception.NoFeasibleSolutionException;
import com.lucadani.backend.optimization.PortfolioConstraint.MaxAssets;
import com.lucadani.backend.optimization.PortfolioConstraint.MinExpectedReturn;
import com.lucadani.backend.optimization.PortfolioConstraint.SectorLimit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.lucadani.backend.optimization.OptimizationProblemTest.SECTORS;
import static com.lucadani.backend.optimization.OptimizationProblemTest.twoAssets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

/**
 * Two uncorrelated assets (variances 0.04 and 0.09, returns 10% and 20%) have closed-form answers:
 * minimum variance weights are proportional to 1 / variance, and the maximum Sharpe weights
 * (risk-free rate 0) are proportional to return / variance.
 */
class MeanVarianceStrategiesTest {

    private static final double SOLVER_TOLERANCE = 1e-5;

    private final MinimumVarianceStrategy minimumVariance = new MinimumVarianceStrategy();
    private final EfficientFrontier frontier = new EfficientFrontier();
    private final MaxSharpeStrategy maxSharpe = new MaxSharpeStrategy(frontier);

    @Test
    void minimumVarianceWeightsAreInverselyProportionalToVariance() {
        double[] weights = minimumVariance.optimize(problem(List.of()));

        assertThat(weights[0]).isCloseTo(0.09 / 0.13, within(SOLVER_TOLERANCE));
        assertThat(weights[1]).isCloseTo(0.04 / 0.13, within(SOLVER_TOLERANCE));
    }

    @Test
    void minimumVarianceMeetsTheTargetReturn() {
        // 0.10 * wA + 0.20 * wB >= 0.18 forces wB >= 0.8; the least risky choice is exactly 0.8
        double[] weights = minimumVariance.optimize(problem(List.of(new MinExpectedReturn(0.18))));

        assertThat(weights[0]).isCloseTo(0.2, within(SOLVER_TOLERANCE));
        assertThat(weights[1]).isCloseTo(0.8, within(SOLVER_TOLERANCE));
    }

    @Test
    void minimumVarianceRespectsSectorLimits() {
        double[] weights = minimumVariance.optimize(problem(List.of(new SectorLimit("Energy", 0.2))));

        assertThat(weights[1]).isCloseTo(0.2, within(SOLVER_TOLERANCE));
    }

    @Test
    void minimumVarianceRespectsTheMaximumNumberOfAssets() {
        OptimizationProblem problem = problem(List.of(new MaxAssets(1)));

        double[] weights = minimumVariance.optimize(problem);

        // With a single asset allowed, the less volatile one (A) is held entirely
        assertThat(weights[0]).isCloseTo(1.0, within(SOLVER_TOLERANCE));
        assertThat(PortfolioEvaluation.evaluate(problem, weights).feasible()).isTrue();
    }

    @Test
    void maxSharpeFindsTheTangencyPortfolio() {
        double[] weights = maxSharpe.optimize(problem(List.of()));

        double a = 0.10 / 0.04;
        double b = 0.20 / 0.09;
        assertThat(weights[0]).isCloseTo(a / (a + b), within(1e-4));
        assertThat(weights[1]).isCloseTo(b / (a + b), within(1e-4));
    }

    @Test
    void frontierGoesFromMinimumVarianceToMaximumReturn() {
        List<FrontierPoint> points = frontier.trace(problem(List.of()), 11);

        assertThat(points).hasSize(11);
        assertThat(points.getFirst().weight(0)).isCloseTo(0.09 / 0.13, within(SOLVER_TOLERANCE));
        assertThat(points.getLast().evaluation().expectedReturn()).isCloseTo(0.20, within(SOLVER_TOLERANCE));
        for (int k = 1; k < points.size(); k++) {
            assertThat(points.get(k).evaluation().expectedReturn())
                    .isGreaterThanOrEqualTo(points.get(k - 1).evaluation().expectedReturn() - SOLVER_TOLERANCE);
            assertThat(points.get(k).evaluation().feasible()).isTrue();
        }
    }

    @Test
    void reportsProblemsWithoutAFeasiblePortfolio() {
        // The highest possible return is 20%
        assertThatExceptionOfType(NoFeasibleSolutionException.class)
                .isThrownBy(() -> minimumVariance.optimize(problem(List.of(new MinExpectedReturn(0.5)))));
    }

    private static OptimizationProblem problem(List<PortfolioConstraint> constraints) {
        return new OptimizationProblem(twoAssets(), SECTORS, constraints, 0);
    }
}
