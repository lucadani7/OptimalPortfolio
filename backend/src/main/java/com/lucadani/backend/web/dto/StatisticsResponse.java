package com.lucadani.backend.web.dto;

import com.lucadani.backend.analytics.MarketStatistics;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Annualized statistics. {@code from} and {@code to} are the first and last trading days
 * common to all assets, which may be narrower than the requested period.
 * Row and column {@code i} of both matrices refer to {@code symbols.get(i)}.
 */
public record StatisticsResponse(List<String> symbols,
                                 LocalDate from,
                                 LocalDate to,
                                 int observations,
                                 List<AssetStatistics> assets,
                                 double[][] covariance,
                                 double[][] correlation) {
    public record AssetStatistics(String symbol, double meanReturn, double volatility) {
    }

    public static StatisticsResponse of(MarketStatistics stats) {
        List<AssetStatistics> assets = IntStream.range(0, stats.assetCount())
                .mapToObj(i -> new AssetStatistics(stats.symbols().get(i), stats.meanReturn(i), stats.volatility(i)))
                .toList();
        return new StatisticsResponse(
                stats.symbols(),
                stats.from(),
                stats.to(),
                stats.observations(),
                assets,
                stats.covariance(),
                stats.correlation()
        );
    }
}
