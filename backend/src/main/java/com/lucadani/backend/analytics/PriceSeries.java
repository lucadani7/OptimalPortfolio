package com.lucadani.backend.analytics;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

/** Closing prices of one asset, ordered by date. */
public record PriceSeries(String symbol, NavigableMap<LocalDate, Double> closes) {
    /** Accepts finite positive prices. Also guards against nulls coming from outside the null-checked code. */
    private static final Predicate<@Nullable Double> VALID_PRICE = close -> close != null && close > 0;

    public PriceSeries {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(closes, "closes must not be null");
        for (Map.Entry<LocalDate, Double> entry : closes.entrySet()) {
            if (VALID_PRICE.negate().test(entry.getValue())) {
                throw new IllegalArgumentException("Prices of " + symbol + " must be positive, got "
                        + entry.getValue() + " on " + entry.getKey());
            }
        }
        closes = Collections.unmodifiableNavigableMap(new TreeMap<>(closes));
    }

    public static PriceSeries of(String symbol, Map<LocalDate, Double> closes) {
        return new PriceSeries(symbol, new TreeMap<>(closes));
    }
}
