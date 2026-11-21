package com.lucadani.backend.web.dto;

import com.lucadani.backend.optimization.OptimizationStrategy;

public record StrategyResponse(String name, String description) {

    public static StrategyResponse of(OptimizationStrategy strategy) {
        return new StrategyResponse(strategy.name(), strategy.description());
    }
}
