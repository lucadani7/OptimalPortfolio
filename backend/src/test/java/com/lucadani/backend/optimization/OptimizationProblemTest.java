package com.lucadani.backend.optimization;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.optimization.PortfolioConstraint.AssetWeightBounds;
import com.lucadani.backend.optimization.PortfolioConstraint.WeightBounds;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OptimizationProblemTest {

    static MarketStatistics twoAssets() {
        // A: 10% return, 20% volatility; B: 20% return, 30% volatility; uncorrelated
        return new MarketStatistics(
                List.of("A", "B"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 1),
                100,
                new double[]{0.10, 0.20},
                new double[]{0.20, 0.30},
                new double[][]{{0.04, 0}, {0, 0.09}},
                new double[][]{{1, 0}, {0, 1}});
    }

    static final Map<String, String> SECTORS = Map.of("A", "Tech", "B", "Energy");

    @Test
    void intersectsGlobalAndAssetBounds() {
        OptimizationProblem problem = new OptimizationProblem(twoAssets(), SECTORS,
                List.of(new WeightBounds(0, 0.6), new AssetWeightBounds("A", 0.1, 0.8)), 0);

        assertThat(problem.lowerBounds()).containsExactly(0.1, 0.0);
        assertThat(problem.upperBounds()).containsExactly(0.6, 0.6);
    }

    @Test
    void rejectsBoundsForUnknownAsset() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OptimizationProblem(twoAssets(), SECTORS,
                        List.of(new AssetWeightBounds("Z", 0, 1)), 0))
                .withMessageContaining("not in the problem");
    }

    @Test
    void rejectsMaximumsBelowFullInvestment() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OptimizationProblem(twoAssets(), SECTORS, List.of(new WeightBounds(0, 0.4)), 0))
                .withMessageContaining("less than 100%");
    }

    @Test
    void rejectsMinimumsAboveFullInvestment() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OptimizationProblem(twoAssets(), SECTORS, List.of(new WeightBounds(0.6, 1)), 0))
                .withMessageContaining("more than 100%");
    }

    @Test
    void rejectsContradictoryBounds() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OptimizationProblem(twoAssets(), SECTORS,
                        List.of(new WeightBounds(0, 0.5), new AssetWeightBounds("A", 0.6, 1)), 0))
                .withMessageContaining("contradictory");
    }

    @Test
    void rejectsMissingSector() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OptimizationProblem(twoAssets(), Map.of("A", "Tech"), List.of(), 0))
                .withMessageContaining("Missing sector");
    }
}
