package com.lucadani.backend.optimization;

import com.lucadani.backend.optimization.RebalancePlan.WeightChange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class RebalancePlanTest {

    @Test
    void nothingToTradeWhenTheWeightsAlreadyMatch() {
        RebalancePlan plan = RebalancePlan.of(List.of("A", "B"), new double[]{0.6, 0.4},
                Map.of("A", 600.0, "B", 400.0), 0);

        assertThat(plan.turnover()).isCloseTo(0, within(1e-12));
        assertThat(plan.changes()).extracting(WeightChange::difference)
                .allSatisfy(difference -> assertThat(difference).isCloseTo(0, within(1e-12)));
    }

    @Test
    void movingTenPercentFromOneAssetToAnother() {
        RebalancePlan plan = RebalancePlan.of(List.of("A", "B"), new double[]{0.5, 0.5},
                Map.of("A", 600.0, "B", 400.0), 0);

        assertThat(plan.turnover()).isCloseTo(0.1, within(1e-12));
        assertThat(plan.changes()).containsExactly(new WeightChange("A", 0.6, 0.5), new WeightChange("B", 0.4, 0.5));
    }

    @Test
    void cashIsInvestedAndAssetsNoLongerWantedAreSold() {
        // Total 1000: 500 in A, 400 in C (not among the symbols any more), 100 in cash
        RebalancePlan plan = RebalancePlan.of(List.of("A", "B"), new double[]{0.5, 0.5},
                Map.of("A", 500.0, "C", 400.0), 100);

        // Sell all of C (40%) and invest the cash (10%): 50% goes into B
        assertThat(plan.turnover()).isCloseTo(0.5, within(1e-12));
        assertThat(plan.changes()).containsExactly(
                new WeightChange("A", 0.5, 0.5), new WeightChange("B", 0, 0.5), new WeightChange("C", 0.4, 0));
    }

    @Test
    void leavesOutAssetsNeitherHeldNorWanted() {
        RebalancePlan plan = RebalancePlan.of(List.of("A", "B"), new double[]{1, 0}, Map.of("A", 100.0), 0);

        assertThat(plan.changes()).extracting(WeightChange::symbol).containsExactly("A");
    }

    @Test
    void rejectsAPortfolioWithoutValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RebalancePlan.of(List.of("A"), new double[]{1}, Map.of(), 0));
    }
}
