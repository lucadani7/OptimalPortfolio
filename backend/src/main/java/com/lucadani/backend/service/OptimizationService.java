package com.lucadani.backend.service;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.model.Asset;
import com.lucadani.backend.optimization.DiscreteAllocation;
import com.lucadani.backend.optimization.DiscreteAllocator;
import com.lucadani.backend.optimization.EfficientFrontier;
import com.lucadani.backend.optimization.OptimizationProblem;
import com.lucadani.backend.optimization.OptimizationResult;
import com.lucadani.backend.optimization.OptimizationStrategy;
import com.lucadani.backend.optimization.PortfolioConstraint;
import com.lucadani.backend.optimization.ProgressListener;
import com.lucadani.backend.repository.AssetRepository;
import com.lucadani.backend.repository.PricePointRepository;
import com.lucadani.backend.repository.PriceRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OptimizationService {
    private final StatisticsService statisticsService;
    private final AssetRepository assetRepository;
    private final PricePointRepository pricePointRepository;
    private final EfficientFrontier efficientFrontier;
    private final DiscreteAllocator discreteAllocator;
    private final Map<String, OptimizationStrategy> strategies;

    public OptimizationService(StatisticsService statisticsService,
                               AssetRepository assetRepository,
                               PricePointRepository pricePointRepository,
                               EfficientFrontier efficientFrontier,
                               DiscreteAllocator discreteAllocator,
                               List<OptimizationStrategy> strategies) {
        this.statisticsService = statisticsService;
        this.assetRepository = assetRepository;
        this.pricePointRepository = pricePointRepository;
        this.efficientFrontier = efficientFrontier;
        this.discreteAllocator = discreteAllocator;
        this.strategies = strategies.stream().collect(Collectors.toMap(
                OptimizationStrategy::name,
                Function.identity(),
                (first, second) -> {
                    throw new IllegalStateException("Duplicate strategy name: " + first.name());
                },
                LinkedHashMap::new));
    }

    public List<OptimizationStrategy> strategies() {
        return List.copyOf(strategies.values());
    }

    public OptimizationRun run(StatisticsQuery query, String strategyName,
                               List<PortfolioConstraint> constraints, double riskFreeRate) {
        return solve(findStrategy(strategyName), buildProblem(query, constraints, riskFreeRate));
    }

    /**
     * Runs a strategy, then turns its weights into whole numbers of shares for the given budget,
     * at the latest prices on or before the end of the period.
     */
    public AllocationRun allocate(StatisticsQuery query, String strategyName,
                                  List<PortfolioConstraint> constraints, double riskFreeRate, double budget) {
        OptimizationStrategy strategy = findStrategy(strategyName);
        OptimizationProblem problem = buildProblem(query, constraints, riskFreeRate);
        OptimizationRun run = solve(strategy, problem);
        List<String> symbols = run.result().symbols();

        Map<String, PriceRow> latest = pricePointRepository.findLatestRows(symbols, query.to()).stream()
                .collect(Collectors.toMap(PriceRow::symbol, Function.identity()));
        double[] prices = new double[symbols.size()];
        List<LocalDate> priceDates = new ArrayList<>(symbols.size());
        for (int i = 0; i < symbols.size(); i++) {
            PriceRow row = latest.get(symbols.get(i));
            if (row == null) {
                throw new IllegalArgumentException("No price available for " + symbols.get(i)
                        + " on or before " + query.to());
            }
            prices[i] = row.close().doubleValue();
            priceDates.add(row.tradeDate());
        }

        // Caps from the problem keep asset and sector limits valid after rounding to whole shares
        DiscreteAllocation allocation = discreteAllocator.allocate(problem, run.result().weights(), prices, budget);
        return new AllocationRun(run, allocation, priceDates);
    }

    /** Traces the efficient frontier for the given assets, period and constraints. */
    public FrontierRun frontier(StatisticsQuery query, List<PortfolioConstraint> constraints,
                                double riskFreeRate, int points) {
        return frontier(query, constraints, riskFreeRate, points, ProgressListener.NONE);
    }

    /** Same as above, reporting each solved point of the frontier to {@code listener}. */
    public FrontierRun frontier(StatisticsQuery query, List<PortfolioConstraint> constraints,
                                double riskFreeRate, int points, ProgressListener listener) {
        OptimizationProblem problem = buildProblem(query, constraints, riskFreeRate);
        return new FrontierRun(problem, efficientFrontier.trace(problem, points, listener));
    }

    private static OptimizationRun solve(OptimizationStrategy strategy, OptimizationProblem problem) {
        long start = System.nanoTime();
        double[] weights = strategy.optimize(problem);
        Duration solveTime = Duration.ofNanos(System.nanoTime() - start);
        return new OptimizationRun(OptimizationResult.of(strategy.name(), problem, weights, solveTime),
                problem.statistics());
    }

    private OptimizationProblem buildProblem(StatisticsQuery query, List<PortfolioConstraint> constraints,
                                             double riskFreeRate) {
        MarketStatistics statistics = statisticsService.getStatistics(query);
        Map<String, String> sectors = assetRepository.findAllBySymbolIn(query.symbols()).stream()
                .collect(Collectors.toMap(Asset::getSymbol, Asset::getSector));
        return new OptimizationProblem(statistics, sectors, constraints, riskFreeRate);
    }

    private OptimizationStrategy findStrategy(String name) {
        OptimizationStrategy strategy = strategies.get(name.trim().toLowerCase(Locale.ROOT));
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown strategy '" + name + "'. Available: "
                    + String.join(", ", strategies.keySet()));
        }
        return strategy;
    }
}
