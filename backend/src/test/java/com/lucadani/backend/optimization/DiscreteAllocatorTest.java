package com.lucadani.backend.optimization;

import com.lucadani.backend.optimization.PortfolioConstraint.SectorLimit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.lucadani.backend.optimization.OptimizationProblemTest.SECTORS;
import static com.lucadani.backend.optimization.OptimizationProblemTest.twoAssets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class DiscreteAllocatorTest {

    private final DiscreteAllocator allocator = new DiscreteAllocator();

    @Test
    void matchesTargetsExactlyWhenPricesDivideTheBudget() {
        DiscreteAllocation allocation = allocator.allocate(
                List.of("A", "B"), new double[]{0.5, 0.5}, new double[]{100, 50}, 1000);

        assertThat(allocation.shares()).containsExactly(5, 10);
        assertThat(allocation.leftover()).isCloseTo(0, within(1e-9));
        assertThat(allocation.optimal()).isTrue();
    }

    @Test
    void beatsGreedyRoundingWhenItMissesTheBestCombination() {
        // Targets are 73.32 and 82.68. Greedy buys 3 x 29 (B only) and leaves A empty;
        // 1 x 106 + 1 x 29 is much closer to both targets.
        DiscreteAllocation allocation = allocator.allocate(
                List.of("A", "B"), new double[]{0.47, 0.53}, new double[]{106, 29}, 156);

        assertThat(DiscreteAllocator.greedyShares(new int[]{10600, 2900}, new int[]{7332, 8268}, 15600))
                .containsExactly(0, 3);
        assertThat(allocation.shares()).containsExactly(1, 1);
        assertThat(allocation.optimal()).isTrue();
    }

    @Test
    void neverExceedsTheBudget() {
        DiscreteAllocation allocation = allocator.allocate(
                List.of("AAPL", "MSFT", "JNJ", "XOM", "GLD"),
                new double[]{0.18, 0.08, 0.12, 0.25, 0.37},
                new double[]{231.45, 412.10, 151.20, 104.80, 187.60},
                10_000);

        assertThat(allocation.invested()).isLessThanOrEqualTo(10_000);
        assertThat(allocation.invested() + allocation.leftover()).isCloseTo(10_000, within(1e-6));
    }

    @Test
    void neverBuysAssetsWithZeroTargetWeight() {
        DiscreteAllocation allocation = allocator.allocate(
                List.of("A", "B", "C"), new double[]{0.6, 0.4, 0}, new double[]{10, 20, 5}, 1000);

        assertThat(allocation.shares()).containsExactly(60, 20, 0);
    }

    @Test
    void skipsAssetsMoreExpensiveThanTheBudget() {
        DiscreteAllocation allocation = allocator.allocate(
                List.of("EXPENSIVE", "CHEAP"), new double[]{0.7, 0.3}, new double[]{700_000, 500}, 5000);

        assertThat(allocation.shares(0)).isZero();
        assertThat(allocation.invested()).isLessThanOrEqualTo(5000);
    }

    @Test
    void keepsSectorLimitsWhenRoundingToWholeShares() {
        // Energy (B) may hold at most 30%. Its target is 300: two shares (320) are closer than one (160),
        // but would put the sector at 32%.
        OptimizationProblem problem = new OptimizationProblem(twoAssets(), SECTORS,
                List.of(new SectorLimit("Energy", 0.3)), 0);
        double[] targets = {0.7, 0.3};
        double[] prices = {10, 160};

        DiscreteAllocation uncapped = allocator.allocate(List.of("A", "B"), targets, prices, 1000);
        DiscreteAllocation capped = allocator.allocate(problem, targets, prices, 1000);

        assertThat(uncapped.shares()).containsExactly(68, 2);
        assertThat(capped.shares()).containsExactly(70, 1);
        assertThat(capped.actualWeight(1)).isLessThanOrEqualTo(0.3);
    }

    @Test
    void rejectsInvalidBudgets() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> allocator.allocate(List.of("A"), new double[]{1}, new double[]{10}, 0))
                .withMessageContaining("budget");
    }
}
