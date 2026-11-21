package com.lucadani.backend.marketdata;

import com.lucadani.backend.web.dto.PriceEntry;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class TiingoMarketDataProviderTest {

    private final TiingoMarketDataProvider provider =
            new TiingoMarketDataProvider("test-key", "https://api.tiingo.com", JsonMapper.builder().build());

    @Test
    void readsAdjustedClosesInDateOrder() {
        String body = """
                [
                  {"date": "2026-11-20T00:00:00.000Z", "close": 230.10, "adjClose": 229.85, "volume": 51000000},
                  {"date": "2026-11-19T00:00:00.000Z", "close": 228.40, "adjClose": 228.15, "volume": 48000000}
                ]
                """;

        assertThat(provider.parse(body)).containsExactly(
                new PriceEntry(LocalDate.of(2026, 11, 19), new BigDecimal("228.15")),
                new PriceEntry(LocalDate.of(2026, 11, 20), new BigDecimal("229.85")));
    }

    @Test
    void skipsDaysWithoutAPositiveAdjustedClose() {
        String body = """
                [
                  {"date": "2026-11-19T00:00:00.000Z", "adjClose": null},
                  {"date": "2026-11-20T00:00:00.000Z", "adjClose": 0}
                ]
                """;

        assertThat(provider.parse(body)).isEmpty();
    }

    @Test
    void requiresAnApiKey() {
        assertThatIllegalStateException().isThrownBy(
                () -> new TiingoMarketDataProvider(" ", "https://api.tiingo.com", JsonMapper.builder().build()));
    }
}
