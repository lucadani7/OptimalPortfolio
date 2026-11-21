package com.lucadani.backend.optimization;

import com.lucadani.backend.optimization.PortfolioConstraint.AssetWeightBounds;
import com.lucadani.backend.optimization.PortfolioConstraint.MaxAssets;
import com.lucadani.backend.optimization.PortfolioConstraint.MinExpectedReturn;
import com.lucadani.backend.optimization.PortfolioConstraint.SectorLimit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.lucadani.backend.optimization.OptimizationProblemTest.SECTORS;
import static com.lucadani.backend.optimization.OptimizationProblemTest.twoAssets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PortfolioEvaluationTest {

    private static final double TOLERANCE = 1e-12;

    @Test
    void equalWeightSplitsTheBudgetEvenly() {
        OptimizationProblem problem = new OptimizationProblem(twoAssets(), SECTORS, List.of(), 0);

        assertThat(new EqualWeightStrategy().optimize(problem)).containsExactly(0.5, 0.5);
    }

    @Test
    void computesReturnVolatilityAndSharpeRatio() {
        // 0.5 * 10% + 0.5 * 20% = 15%; variance 0.25 * 0.04 + 0.25 * 0.09 = 0.0325
        OptimizationProblem problem = new OptimizationProblem(twoAssets(), SECTORS, List.of(), 0.02);

        PortfolioEvaluation evaluation = PortfolioEvaluation.evaluate(problem, new double[]{0.5, 0.5});

        assertThat(evaluation.expectedReturn()).isCloseTo(0.15, within(TOLERANCE));
        assertThat(evaluation.volatility()).isCloseTo(Math.sqrt(0.0325), within(TOLERANCE));
        assertThat(evaluation.sharpeRatio()).isCloseTo(0.13 / Math.sqrt(0.0325), within(TOLERANCE));
        assertThat(evaluation.feasible()).isTrue();
    }

    @Test
    void reportsEveryBrokenConstraint() {
        OptimizationProblem problem = new OptimizationProblem(twoAssets(), SECTORS, List.of(
                new AssetWeightBounds("A", 0, 0.4),
                new SectorLimit("Energy", 0.3),
                new MaxAssets(1),
                new MinExpectedReturn(0.2)), 0);

        PortfolioEvaluation evaluation = PortfolioEvaluation.evaluate(problem, new double[]{0.5, 0.5});

        assertThat(evaluation.feasible()).isFalse();
        assertThat(evaluation.violations()).containsExactly(
                "A weight 0.5000 is above the maximum 0.4000",
                "Sector Energy weight 0.5000 is above the maximum 0.3000",
                "Portfolio holds 2 assets, more than the maximum 1",
                "Expected return 0.1500 is below the minimum 0.2000");
    }

    @Test
    void reportsWeightsThatDoNotAddUpToOne() {
        OptimizationProblem problem = new OptimizationProblem(twoAssets(), SECTORS, List.of(), 0);

        PortfolioEvaluation evaluation = PortfolioEvaluation.evaluate(problem, new double[]{0.5, 0.4});

        assertThat(evaluation.violations()).containsExactly("Weights add up to 0.9000 instead of 1");
    }
}
