package com.lucadani.backend.web.dto;

import com.lucadani.backend.optimization.DiscreteAllocation;
import com.lucadani.backend.optimization.OptimizationResult;
import com.lucadani.backend.optimization.PortfolioEvaluation;
import com.lucadani.backend.service.AllocationRun;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Whole-share allocation of an optimized portfolio.
 * {@code expectedReturn}, {@code volatility} and {@code sharpeRatio} describe the target portfolio;
 * {@code optimal} is false when the allocation search stopped at its time limit.
 */
public record AllocationResponse(String strategy,
                                double budget,
                                double invested,
                                double leftover,
                                List<Position> positions,
                                double expectedReturn,
                                double volatility,
                                double sharpeRatio,
                                boolean feasible,
                                List<String> violations,
                                boolean optimal,
                                double solveTimeMillis) {

    public record Position(String symbol, int shares, double price, LocalDate priceDate, double amount, double targetWeight, double actualWeight) {
    }

    public static AllocationResponse of(AllocationRun run) {
        OptimizationResult result = run.optimization().result();
        PortfolioEvaluation evaluation = result.evaluation();
        DiscreteAllocation allocation = run.allocation();

        List<Position> positions = IntStream.range(0, allocation.symbols().size())
                .mapToObj(i -> new Position(
                        allocation.symbols().get(i),
                        allocation.shares(i),
                        allocation.price(i),
                        run.priceDates().get(i),
                        allocation.shares(i) * allocation.price(i),
                        allocation.targetWeight(i),
                        allocation.actualWeight(i)))
                .toList();

        return new AllocationResponse(
                result.strategy(),
                allocation.budget(),
                allocation.invested(),
                allocation.leftover(),
                positions,
                evaluation.expectedReturn(),
                evaluation.volatility(),
                evaluation.sharpeRatio(),
                evaluation.feasible(),
                evaluation.violations(),
                allocation.optimal(),
                (result.solveTime().toNanos() + allocation.solveTime().toNanos()) / 1_000_000.0
        );
    }
}
