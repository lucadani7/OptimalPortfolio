package com.lucadani.backend.web;

import com.lucadani.backend.service.OptimizationService;
import com.lucadani.backend.service.StatisticsQuery;
import com.lucadani.backend.web.dto.AllocationRequest;
import com.lucadani.backend.web.dto.AllocationResponse;
import com.lucadani.backend.web.dto.FrontierRequest;
import com.lucadani.backend.web.dto.FrontierResponse;
import com.lucadani.backend.web.dto.OptimizationRequest;
import com.lucadani.backend.web.dto.OptimizationResponse;
import com.lucadani.backend.web.dto.StrategyResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/optimization")
@RequiredArgsConstructor
@Tag(name = "Optimization", description = "Build portfolios with different strategies")
public class OptimizationController {

    private final OptimizationService optimizationService;

    @GetMapping("/strategies")
    public List<StrategyResponse> strategies() {
        return optimizationService.strategies().stream().map(StrategyResponse::of).toList();
    }

    @PostMapping("/run")
    public OptimizationResponse run(@Valid @RequestBody OptimizationRequest request) {
        StatisticsQuery query = StatisticsQuery.of(request.symbols(), request.from(), request.to());
        return OptimizationResponse.of(optimizationService.run(
                query, request.strategy(), request.domainConstraints(), request.riskFreeRateOrZero()));
    }

    @PostMapping("/allocate")
    public AllocationResponse allocate(@Valid @RequestBody AllocationRequest request) {
        StatisticsQuery query = StatisticsQuery.of(request.symbols(), request.from(), request.to());
        return AllocationResponse.of(optimizationService.allocate(
                query, request.strategy(), request.domainConstraints(), request.riskFreeRateOrZero(),
                request.budget()));
    }

    @PostMapping("/frontier")
    public FrontierResponse frontier(@Valid @RequestBody FrontierRequest request) {
        StatisticsQuery query = StatisticsQuery.of(request.symbols(), request.from(), request.to());
        return FrontierResponse.of(optimizationService.frontier(
                query, request.domainConstraints(), request.riskFreeRateOrZero(), request.pointsOrDefault()));
    }
}
