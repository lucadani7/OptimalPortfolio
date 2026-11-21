package com.lucadani.backend.analytics;

import java.time.LocalDate;
import java.util.*;

/**
 * Prices of several assets restricted to the dates on which every asset has a price.
 * {@code prices[asset][day]} is the close of {@code symbols.get(asset)} on {@code dates.get(day)}.
 */
public record AlignedPrices(List<String> symbols, List<LocalDate> dates, double[][] prices) {
    public static AlignedPrices align(List<PriceSeries> series) {
        if (series.isEmpty()) {
            throw new IllegalArgumentException("At least one price series is required");
        }
        Set<String> seen = new HashSet<>();
        for (PriceSeries s : series) {
            if (!seen.add(s.symbol())) {
                throw new IllegalArgumentException("Duplicate symbol: " + s.symbol());
            }
        }

        // Keep only the dates present in every series
        NavigableSet<LocalDate> common = new TreeSet<>(series.getFirst().closes().keySet());
        for (PriceSeries s : series.subList(1, series.size())) {
            common.retainAll(s.closes().keySet());
        }

        List<LocalDate> dates = List.copyOf(common);
        double[][] prices = new double[series.size()][dates.size()];
        for (int a = 0; a < series.size(); a++) {
            NavigableMap<LocalDate, Double> closes = series.get(a).closes();
            for (int d = 0; d < dates.size(); d++) {
                prices[a][d] = closes.get(dates.get(d));
            }
        }

        List<String> symbols = series.stream().map(PriceSeries::symbol).toList();
        return new AlignedPrices(symbols, dates, prices);
    }

    public int assetCount() {
        return symbols.size();
    }

    public int dayCount() {
        return dates.size();
    }
}
