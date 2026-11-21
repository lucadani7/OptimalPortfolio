package com.lucadani.backend.web.dto;

import com.lucadani.backend.optimization.OptimizationResult;
import com.lucadani.backend.optimization.PortfolioEvaluation;
import com.lucadani.backend.service.OptimizationRun;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Result of an optimization. {@code from} and {@code to} are the first and last trading days
 * the statistics are based on. When {@code feasible} is false, {@code violations} lists the broken constraints.
 */
public record OptimizationResponse(String strategy,
                                   LocalDate from,
                                   LocalDate to,
                                   int observations,
                                   List<Allocation> allocations,
                                   double expectedReturn,
                                   double volatility,
                                   double sharpeRatio,
                                   boolean feasible,
                                   List<String> violations,
                                   double solveTimeMillis) {

    public record Allocation(String symbol, double weight) {
    }

    public static OptimizationResponse of(OptimizationRun run) {
        OptimizationResult result = run.result();
        PortfolioEvaluation evaluation = result.evaluation();
        List<Allocation> allocations = IntStream.range(0, result.symbols().size())
                .mapToObj(i -> new Allocation(result.symbols().get(i), result.weight(i)))
                .toList();

        return new OptimizationResponse(
                result.strategy(),
                run.statistics().from(),
                run.statistics().to(),
                run.statistics().observations(),
                allocations,
                evaluation.expectedReturn(),
                evaluation.volatility(),
                evaluation.sharpeRatio(),
                evaluation.feasible(),
                evaluation.violations(),
                result.solveTime().toNanos() / 1_000_000.0
        );
    }
}
