package com.lucadani.backend.web.dto;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.optimization.FrontierPoint;
import com.lucadani.backend.optimization.PortfolioEvaluation;
import com.lucadani.backend.service.FrontierRun;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The efficient frontier, ready to plot as volatility (x) against expected return (y).
 * {@code assets} are the individual assets, to draw next to the curve.
 * In every point, {@code weights[i]} is the weight of {@code symbols.get(i)}.
 */
public record FrontierResponse(
        List<String> symbols,
        LocalDate from,
        LocalDate to,
        int observations,
        List<AssetPoint> assets,
        List<Point> points
) {

    public record AssetPoint(String symbol, double expectedReturn, double volatility) {
    }

    public record Point(double expectedReturn, double volatility, double sharpeRatio, double[] weights) {
    }

    public static FrontierResponse of(FrontierRun run) {
        MarketStatistics stats = run.problem().statistics();

        List<AssetPoint> assets = IntStream.range(0, stats.assetCount())
                .mapToObj(i -> new AssetPoint(stats.symbols().get(i), stats.meanReturn(i), stats.volatility(i)))
                .toList();

        List<Point> points = run.points().stream()
                .map(FrontierResponse::point)
                .toList();

        return new FrontierResponse(stats.symbols(), stats.from(), stats.to(), stats.observations(), assets, points);
    }

    private static Point point(FrontierPoint frontierPoint) {
        PortfolioEvaluation evaluation = frontierPoint.evaluation();
        return new Point(evaluation.expectedReturn(), evaluation.volatility(), evaluation.sharpeRatio(),
                frontierPoint.weights());
    }
}
