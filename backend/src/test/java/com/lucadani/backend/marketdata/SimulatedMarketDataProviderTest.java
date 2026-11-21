package com.lucadani.backend.marketdata;

import com.lucadani.backend.model.AssetType;
import com.lucadani.backend.web.dto.PriceEntry;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedMarketDataProviderTest {

    // A Monday, so the two weeks below hold exactly ten trading days
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private static final List<LocalDate> TWO_WEEKS = TradingCalendar.tradingDays(MONDAY, MONDAY.plusDays(13));

    @Test
    void tradingDaysSkipWeekends() {
        assertThat(TWO_WEEKS).hasSize(10)
                .noneMatch(day -> day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY);
        assertThat(TradingCalendar.tradingDays(MONDAY, MONDAY.minusDays(1))).isEmpty();
    }

    @Test
    void producesOnePositivePricePerTradingDay() {
        List<PriceEntry> prices = SimulatedMarketDataProvider.simulate("AAPL", AssetType.STOCK, 180, TWO_WEEKS);

        assertThat(prices).extracting(PriceEntry::date).containsExactlyElementsOf(TWO_WEEKS);
        assertThat(prices).allMatch(price -> price.close().signum() > 0);
    }

    @Test
    void sameInputsGiveSamePrices() {
        List<PriceEntry> first = SimulatedMarketDataProvider.simulate("AAPL", AssetType.STOCK, 180, TWO_WEEKS);
        List<PriceEntry> second = SimulatedMarketDataProvider.simulate("AAPL", AssetType.STOCK, 180, TWO_WEEKS);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void differentSymbolsMoveDifferently() {
        List<PriceEntry> apple = SimulatedMarketDataProvider.simulate("AAPL", AssetType.STOCK, 100, TWO_WEEKS);
        List<PriceEntry> microsoft = SimulatedMarketDataProvider.simulate("MSFT", AssetType.STOCK, 100, TWO_WEEKS);

        assertThat(microsoft).isNotEqualTo(apple);
    }

    @Test
    void bondsStayCloseToTheirStartingPrice() {
        List<LocalDate> year = TradingCalendar.tradingDays(MONDAY, MONDAY.plusYears(1));
        List<PriceEntry> prices = SimulatedMarketDataProvider.simulate("BND", AssetType.BOND, 72, year);

        assertThat(prices).allMatch(price -> Math.abs(price.close().doubleValue() / 72 - 1) < 0.10);
    }
}
