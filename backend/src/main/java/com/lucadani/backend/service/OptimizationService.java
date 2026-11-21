package com.lucadani.backend.service;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.model.Asset;
import com.lucadani.backend.optimization.OptimizationProblem;
import com.lucadani.backend.optimization.OptimizationResult;
import com.lucadani.backend.optimization.OptimizationStrategy;
import com.lucadani.backend.optimization.PortfolioConstraint;
import com.lucadani.backend.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    private final Map<String, OptimizationStrategy> strategies;

    public OptimizationService(StatisticsService statisticsService,
                               AssetRepository assetRepository,
                               List<OptimizationStrategy> strategies) {
        this.statisticsService = statisticsService;
        this.assetRepository = assetRepository;
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
        OptimizationStrategy strategy = findStrategy(strategyName);

        MarketStatistics statistics = statisticsService.getStatistics(query);
        Map<String, String> sectors = assetRepository.findAllBySymbolIn(query.symbols()).stream()
                .collect(Collectors.toMap(Asset::getSymbol, Asset::getSector));
        OptimizationProblem problem = new OptimizationProblem(statistics, sectors, constraints, riskFreeRate);

        long start = System.nanoTime();
        double[] weights = strategy.optimize(problem);
        Duration solveTime = Duration.ofNanos(System.nanoTime() - start);

        return new OptimizationRun(OptimizationResult.of(strategy.name(), problem, weights, solveTime), statistics);
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
