package com.lucadani.backend.service;

import com.lucadani.backend.optimization.DiscreteAllocation;

import java.time.LocalDate;
import java.util.List;

/**
 * An optimization together with the whole-share allocation of its weights.
 * {@code priceDates.get(i)} is the trading day of the price used for asset {@code i}.
 */
public record AllocationRun(OptimizationRun optimization, DiscreteAllocation allocation, List<LocalDate> priceDates) {

    public AllocationRun {
        priceDates = List.copyOf(priceDates);
    }
}
