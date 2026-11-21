package com.lucadani.backend.service;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Normalized parameters of a statistics request. Also used as the cache key,
 * so equal queries share the same cached result.
 */
public record StatisticsQuery(List<String> symbols, LocalDate from, LocalDate to) {
    public static final int MAX_ASSETS = 50;
    public static final int DEFAULT_LOOKBACK_YEARS = 1;

    public StatisticsQuery {
        symbols = List.copyOf(symbols);
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("At least one symbol is required");
        }
        if (symbols.size() > MAX_ASSETS) {
            throw new IllegalArgumentException("At most " + MAX_ASSETS + " symbols are allowed");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
    }

    /**
     * Normalizes symbols (trimmed, upper case, duplicates removed, order kept)
     * and fills in the default period: the last year up to today.
     */
    public static StatisticsQuery of(List<String> symbols, @Nullable LocalDate from, @Nullable LocalDate to) {
        List<String> normalized = symbols.stream()
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .filter(symbol -> !symbol.isEmpty())
                .distinct()
                .toList();
        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusYears(DEFAULT_LOOKBACK_YEARS);
        return new StatisticsQuery(normalized, start, end);
    }
}
