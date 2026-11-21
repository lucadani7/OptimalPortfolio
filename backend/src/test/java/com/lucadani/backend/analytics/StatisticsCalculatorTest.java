package com.lucadani.backend.analytics;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

public class StatisticsCalculatorTest {
    private static final double TOLERANCE = 1e-12;
    private static final LocalDate START = LocalDate.of(2026, 1, 5);

    /** No annualization, so expected values can be computed by hand. */
    private final StatisticsCalculator calculator = new StatisticsCalculator(1);

    @Test
    void computesSimpleReturnsFromConsecutivePrices() {
        double[][] returns = StatisticsCalculator.simpleReturns(new double[][]{{100, 110, 99}});

        assertThat(returns[0][0]).isCloseTo(0.10, within(TOLERANCE));
        assertThat(returns[0][1]).isCloseTo(-0.10, within(TOLERANCE));
    }

    @Test
    void computesMeanVarianceAndVolatility() {
        // A: +10%, -10%  ->  mean 0, sample variance (0.01 + 0.01) / 1 = 0.02
        // B: +5%, +5%    ->  mean 0.05, variance 0
        MarketStatistics stats = calculator.compute(List.of(
                series("A", 100, 110, 99),
                series("B", 100, 105, 110.25)));

        assertThat(stats.observations()).isEqualTo(2);
        assertThat(stats.meanReturn(0)).isCloseTo(0.0, within(TOLERANCE));
        assertThat(stats.covariance(0, 0)).isCloseTo(0.02, within(TOLERANCE));
        assertThat(stats.volatility(0)).isCloseTo(Math.sqrt(0.02), within(TOLERANCE));
        assertThat(stats.meanReturn(1)).isCloseTo(0.05, within(TOLERANCE));
        assertThat(stats.covariance(1, 1)).isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    void assetWithZeroVarianceIsUncorrelated() {
        MarketStatistics stats = calculator.compute(List.of(
                series("A", 100, 110, 99),
                series("B", 100, 105, 110.25)));

        assertThat(stats.correlation(0, 1)).isEqualTo(0.0);
        assertThat(stats.correlation(1, 1)).isEqualTo(1.0);
    }

    @Test
    void assetsWithIdenticalReturnsArePerfectlyCorrelated() {
        // B is always twice A, so their returns are identical
        MarketStatistics stats = calculator.compute(List.of(
                series("A", 100, 110, 99, 108.9),
                series("B", 200, 220, 198, 217.8)));

        assertThat(stats.correlation(0, 1)).isCloseTo(1.0, within(TOLERANCE));
    }

    @Test
    void assetsWithOppositeReturnsArePerfectlyAnticorrelated() {
        MarketStatistics stats = calculator.compute(List.of(
                series("A", 100, 110, 99, 108.9),
                series("B", 100, 90, 99, 89.1)));

        assertThat(stats.correlation(0, 1)).isCloseTo(-1.0, within(TOLERANCE));
    }

    @Test
    void matricesAreSymmetric() {
        MarketStatistics stats = calculator.compute(List.of(
                series("A", 100, 110, 99, 108.9, 104),
                series("B", 50, 51, 49, 52, 53),
                series("C", 20, 19, 21, 20.5, 22)));

        for (int i = 0; i < stats.assetCount(); i++) {
            for (int j = 0; j < stats.assetCount(); j++) {
                assertThat(stats.covariance(i, j)).isEqualTo(stats.covariance(j, i));
                assertThat(stats.correlation(i, j)).isEqualTo(stats.correlation(j, i));
            }
        }
    }

    @Test
    void keepsOnlyDatesCommonToAllAssets() {
        // B has no price on the second day
        Map<LocalDate, Double> b = new TreeMap<>();
        b.put(START, 50.0);
        b.put(START.plusDays(2), 55.0);
        b.put(START.plusDays(3), 60.0);

        MarketStatistics stats = calculator.compute(List.of(
                series("A", 100, 110, 99, 108.9),
                PriceSeries.of("B", b)));

        assertThat(stats.observations()).isEqualTo(2);
        assertThat(stats.from()).isEqualTo(START);
        assertThat(stats.to()).isEqualTo(START.plusDays(3));
    }

    @Test
    void annualizesMeansAndCovariancesButNotCorrelations() {
        List<PriceSeries> series = List.of(
                series("A", 100, 110, 99, 108.9),
                series("B", 100, 90, 99, 89.1));

        MarketStatistics daily = new StatisticsCalculator(1).compute(series);
        MarketStatistics annual = new StatisticsCalculator().compute(series);

        assertThat(annual.meanReturn(0)).isCloseTo(daily.meanReturn(0) * 252, within(TOLERANCE));
        assertThat(annual.covariance(0, 1)).isCloseTo(daily.covariance(0, 1) * 252, within(TOLERANCE));
        assertThat(annual.correlation(0, 1)).isCloseTo(daily.correlation(0, 1), within(TOLERANCE));
    }

    @Test
    void rejectsTooFewCommonDays() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> calculator.compute(List.of(series("A", 100, 110))))
                .withMessageContaining("common trading days");
    }

    @Test
    void rejectsDuplicateSymbols() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> calculator.compute(List.of(series("A", 1, 2, 3), series("A", 1, 2, 3))))
                .withMessageContaining("Duplicate symbol");
    }

    @Test
    void rejectsNonPositivePrices() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> series("A", 100, -2, 3))
                .withMessageContaining("must be positive");
    }

    private static PriceSeries series(String symbol, double... closes) {
        Map<LocalDate, Double> prices = new TreeMap<>();
        for (int i = 0; i < closes.length; i++) {
            prices.put(START.plusDays(i), closes[i]);
        }
        return PriceSeries.of(symbol, prices);
    }
}
