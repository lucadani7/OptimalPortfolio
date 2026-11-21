package com.lucadani.backend.service;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.exception.ResourceNotFoundException;
import com.lucadani.backend.jobs.ReoptimizationJob;
import com.lucadani.backend.model.Asset;
import com.lucadani.backend.model.Portfolio;
import com.lucadani.backend.model.PortfolioMetrics;
import com.lucadani.backend.optimization.DiscreteAllocation;
import com.lucadani.backend.optimization.OptimizationResult;
import com.lucadani.backend.optimization.PortfolioEvaluation;
import com.lucadani.backend.repository.AssetRepository;
import com.lucadani.backend.repository.PortfolioRepository;
import com.lucadani.backend.repository.PricePointRepository;
import com.lucadani.backend.repository.PriceRow;
import com.lucadani.backend.web.dto.AllocationRequest;
import com.lucadani.backend.web.dto.ConstraintRequest;
import com.lucadani.backend.web.dto.CreatePortfolioRequest;
import com.lucadani.backend.web.dto.PortfolioResponse;
import com.lucadani.backend.web.dto.PortfolioSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private static final TypeReference<List<ConstraintRequest>> CONSTRAINT_LIST = new TypeReference<>() {
    };

    private final OptimizationService optimizationService;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final PricePointRepository pricePointRepository;
    private final JsonMapper jsonMapper;
    private final JobScheduler jobScheduler;

    public List<PortfolioSummaryResponse> findAll() {
        return portfolioRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PortfolioSummaryResponse::of)
                .toList();
    }

    public PortfolioResponse findById(Long id) {
        return toResponse(getPortfolio(id));
    }

    /** Computes the allocation described by the request and saves it as a new portfolio. */
    @Transactional
    public PortfolioResponse create(CreatePortfolioRequest request) {
        AllocationRequest allocationRequest = request.allocation();
        StatisticsQuery query = StatisticsQuery.of(
                allocationRequest.symbols(), allocationRequest.from(), allocationRequest.to());
        AllocationRun run = optimizationService.allocate(query, allocationRequest.strategy(),
                allocationRequest.domainConstraints(), allocationRequest.riskFreeRateOrZero(),
                allocationRequest.budget());

        OptimizationResult result = run.optimization().result();
        PortfolioEvaluation evaluation = result.evaluation();
        MarketStatistics statistics = run.optimization().statistics();
        DiscreteAllocation allocation = run.allocation();

        PortfolioMetrics metrics = new PortfolioMetrics(
                evaluation.expectedReturn(),
                evaluation.volatility(),
                evaluation.sharpeRatio(),
                allocationRequest.riskFreeRateOrZero(),
                statistics.from(),
                statistics.to());
        List<ConstraintRequest> constraints =
                (allocationRequest.constraints() == null) ? List.of() : allocationRequest.constraints();

        Portfolio portfolio = new Portfolio(
                request.name(),
                result.strategy(),
                money(allocation.budget()),
                money(allocation.invested()),
                money(allocation.leftover()),
                metrics,
                jsonMapper.writerFor(CONSTRAINT_LIST).writeValueAsString(constraints),
                jsonMapper.writeValueAsString(result.symbols()));

        Map<String, Asset> assets = assetRepository.findAllBySymbolIn(allocation.symbols()).stream()
                .collect(Collectors.toMap(Asset::getSymbol, Function.identity()));
        for (int i = 0; i < allocation.symbols().size(); i++) {
            if (allocation.shares(i) > 0) {
                portfolio.addHolding(
                        assets.get(allocation.symbols().get(i)),
                        allocation.shares(i),
                        BigDecimal.valueOf(allocation.price(i)),
                        run.priceDates().get(i),
                        allocation.targetWeight(i));
            }
        }

        return toResponse(portfolioRepository.save(portfolio));
    }

    @Transactional
    public PortfolioResponse rename(Long id, String name) {
        Portfolio portfolio = getPortfolio(id);
        portfolio.rename(name);
        // Flush so that the version and updatedAt values in the response are current
        portfolioRepository.flush();
        return toResponse(portfolio);
    }

    @Transactional
    public void delete(Long id) {
        portfolioRepository.delete(getPortfolio(id));
        jobScheduler.deleteRecurringJob(ReoptimizationJob.recurringJobId(id));
    }

    private Portfolio getPortfolio(Long id) {
        return portfolioRepository.findWithHoldingsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", id));
    }

    private PortfolioResponse toResponse(Portfolio portfolio) {
        List<String> symbols = portfolio.getHoldings().stream()
                .map(holding -> holding.getAsset().getSymbol())
                .toList();
        Map<String, PriceRow> latestPrices = symbols.isEmpty()
                ? Map.of()
                : pricePointRepository.findLatestRows(symbols, LocalDate.now()).stream()
                .collect(Collectors.toMap(PriceRow::symbol, Function.identity()));

        List<ConstraintRequest> constraints = jsonMapper.readValue(portfolio.getConstraintsJson(), CONSTRAINT_LIST);
        return PortfolioResponse.of(portfolio, constraints, latestPrices);
    }

    private static BigDecimal money(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
