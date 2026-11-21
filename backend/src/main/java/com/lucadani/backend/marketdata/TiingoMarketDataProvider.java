package com.lucadani.backend.marketdata;

import com.lucadani.backend.model.Asset;
import com.lucadani.backend.web.dto.PriceEntry;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Real end-of-day prices from Tiingo (tiingo.com). Uses the adjusted close, corrected for splits and
 * dividends, so that daily returns reflect what a holder actually earned. One request per asset.
 * The free plan allows 50 requests an hour and 1,000 a day, for personal use only.
 */
@Component
@ConditionalOnProperty(name = "app.market-data.provider", havingValue = "tiingo")
public class TiingoMarketDataProvider implements MarketDataProvider {

    private static final TypeReference<List<TiingoPrice>> PRICE_LIST = new TypeReference<>() {
    };

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public TiingoMarketDataProvider(@Value("${app.market-data.tiingo.api-key:}") String apiKey,
                                    @Value("${app.market-data.tiingo.base-url}") String baseUrl,
                                    JsonMapper jsonMapper) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("The tiingo market data provider needs an API key: "
                    + "set the TIINGO_API_KEY environment variable");
        }
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Token " + apiKey)
                .build();
        this.jsonMapper = jsonMapper;
    }

    /** One day of Tiingo's end-of-day prices; the other fields of the response are ignored. */
    record TiingoPrice(String date, @Nullable BigDecimal adjClose) {
    }

    @Override
    public String name() {
        return "tiingo";
    }

    @Override
    public List<PriceEntry> fetchPrices(Asset asset, LocalDate from, LocalDate to) {
        String body = restClient.get()
                .uri(uri -> uri.path("/tiingo/daily/{ticker}/prices")
                        .queryParam("startDate", from)
                        .queryParam("endDate", to)
                        .build(asset.getSymbol()))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
        return parse(Objects.requireNonNullElse(body, "[]"));
    }

    /** Package-private for tests: turns a Tiingo response into prices ordered by date. */
    List<PriceEntry> parse(String body) {
        return jsonMapper.readValue(body, PRICE_LIST).stream()
                .filter(price -> price.adjClose() != null && price.adjClose().signum() > 0)
                // Dates come as midnight UTC timestamps, e.g. "2026-11-20T00:00:00.000Z"
                .map(price -> new PriceEntry(LocalDate.parse(price.date().substring(0, 10)), price.adjClose()))
                .sorted(Comparator.comparing(PriceEntry::date))
                .toList();
    }
}
