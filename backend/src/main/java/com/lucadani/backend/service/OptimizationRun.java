package com.lucadani.backend.service;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.optimization.OptimizationResult;

/** The result of an optimization together with the statistics it was based on. */
public record OptimizationRun(OptimizationResult result, MarketStatistics statistics) {
}
