package com.lucadani.backend.service;

import com.lucadani.backend.exception.NoFeasibleSolutionException;
import com.lucadani.backend.exception.ResourceNotFoundException;
import com.lucadani.backend.jobs.ReoptimizationJob;
import com.lucadani.backend.model.Holding;
import com.lucadani.backend.model.Portfolio;
import com.lucadani.backend.model.PortfolioMetrics;
import com.lucadani.backend.model.Reoptimization;
import com.lucadani.backend.model.ReoptimizationSchedule;
import com.lucadani.backend.optimization.OptimizationResult;
import com.lucadani.backend.optimization.PortfolioConstraint;
import com.lucadani.backend.optimization.PortfolioEvaluation;
import com.lucadani.backend.optimization.RebalancePlan;
import com.lucadani.backend.repository.PortfolioRepository;
import com.lucadani.backend.repository.PricePointRepository;
import com.lucadani.backend.repository.PriceRow;
import com.lucadani.backend.repository.ReoptimizationRepository;
import com.lucadani.backend.web.dto.ConstraintRequest;
import com.lucadani.backend.web.dto.ReoptimizationResponse;
import com.lucadani.backend.web.dto.ReoptimizationResponse.WeightChange;
import org.jobrunr.scheduling.JobScheduler;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Runs a saved portfolio's strategy again, with the same assets, rules and length of price history,
 * but ending today, and records how far the portfolio has drifted from the new weights.
 * Nothing is bought or sold: the result is advice to act on or not.
 */
@Service
public class ReoptimizationService {

    private static final TypeReference<List<ConstraintRequest>> CONSTRAINT_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> SYMBOL_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<WeightChange>> CHANGE_LIST = new TypeReference<>() {
    };

    private final PortfolioRepository portfolioRepository;
    private final ReoptimizationRepository reoptimizationRepository;
    private final OptimizationService optimizationService;
    private final PricePointRepository pricePointRepository;
    private final JobScheduler jobScheduler;
    private final JsonMapper jsonMapper;
    private final Clock clock;
    private final ZoneId scheduleZone;

    public ReoptimizationService(PortfolioRepository portfolioRepository,
                                 ReoptimizationRepository reoptimizationRepository,
                                 OptimizationService optimizationService,
                                 PricePointRepository pricePointRepository,
                                 JobScheduler jobScheduler,
                                 JsonMapper jsonMapper,
                                 Clock clock,
                                 @Value("${app.market-data.zone-id}") ZoneId scheduleZone) {
        this.portfolioRepository = portfolioRepository;
        this.reoptimizationRepository = reoptimizationRepository;
        this.optimizationService = optimizationService;
        this.pricePointRepository = pricePointRepository;
        this.jobScheduler = jobScheduler;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
        this.scheduleZone = scheduleZone;
    }

    /**
     * Reoptimizes now and saves the outcome, a failure included (e.g. when the rules can no longer be met).
     * Not transactional: a failing optimization must not roll back the record of its failure.
     */
    public ReoptimizationResponse reoptimize(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findWithHoldingsById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
        Reoptimization reoptimization;
        try {
            reoptimization = run(portfolio);
        } catch (NoFeasibleSolutionException | IllegalArgumentException | ResourceNotFoundException e) {
            reoptimization = Reoptimization.failed(portfolioId,
                    Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()), clock.instant());
        }
        return toResponse(reoptimizationRepository.save(reoptimization));
    }

    public List<ReoptimizationResponse> history(Long portfolioId) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new ResourceNotFoundException("Portfolio", portfolioId);
        }
        return reoptimizationRepository.findTop20ByPortfolioIdOrderByCreatedAtDescIdDesc(portfolioId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Sets how often the portfolio is reoptimized on its own, as a JobRunr recurring job; null stops it. */
    @Transactional
    public void schedule(Long portfolioId, @Nullable ReoptimizationSchedule schedule) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
        portfolio.scheduleReoptimization(schedule);

        String recurringJobId = ReoptimizationJob.recurringJobId(portfolioId);
        if (schedule == null) {
            jobScheduler.deleteRecurringJob(recurringJobId);
        } else {
            jobScheduler.<ReoptimizationJob>scheduleRecurrently(recurringJobId, schedule.cron(), scheduleZone,
                    job -> job.run(portfolioId));
        }
    }

    private Reoptimization run(Portfolio portfolio) {
        // The same length of history as the original optimization, ending today
        PortfolioMetrics metrics = portfolio.getMetrics();
        long lookbackDays = ChronoUnit.DAYS.between(metrics.statisticsFrom(), metrics.statisticsTo());
        LocalDate today = LocalDate.now(clock);
        StatisticsQuery query = StatisticsQuery.of(candidateSymbols(portfolio), today.minusDays(lookbackDays), today);

        List<PortfolioConstraint> constraints = jsonMapper.readValue(portfolio.getConstraintsJson(), CONSTRAINT_LIST)
                .stream()
                .map(ConstraintRequest::toDomain)
                .toList();
        OptimizationRun run = optimizationService.run(query, portfolio.getStrategy(), constraints,
                metrics.riskFreeRate());
        OptimizationResult result = run.result();

        RebalancePlan plan = RebalancePlan.of(result.symbols(), result.weights(), currentValues(portfolio, today),
                portfolio.getLeftover().doubleValue());
        PortfolioEvaluation evaluation = result.evaluation();
        return Reoptimization.succeeded(portfolio.getId(), evaluation.expectedReturn(), evaluation.volatility(),
                evaluation.sharpeRatio(), run.statistics().from(), run.statistics().to(), plan.turnover(),
                jsonMapper.writeValueAsString(plan.changes()), clock.instant());
    }

    /** The assets the portfolio was optimized over; the held ones for portfolios saved before they were kept. */
    private List<String> candidateSymbols(Portfolio portfolio) {
        String symbolsJson = portfolio.getSymbolsJson();
        if (symbolsJson != null) {
            return jsonMapper.readValue(symbolsJson, SYMBOL_LIST);
        }
        return portfolio.getHoldings().stream().map(holding -> holding.getAsset().getSymbol()).toList();
    }

    /** Market value of each holding at its latest price, or at cost when no price is known. */
    private Map<String, Double> currentValues(Portfolio portfolio, LocalDate today) {
        List<String> held = portfolio.getHoldings().stream().map(holding -> holding.getAsset().getSymbol()).toList();
        Map<String, PriceRow> latest = held.isEmpty() ? Map.of()
                : pricePointRepository.findLatestRows(held, today).stream()
                .collect(Collectors.toMap(PriceRow::symbol, Function.identity()));

        Map<String, Double> values = new LinkedHashMap<>();
        for (Holding holding : portfolio.getHoldings()) {
            PriceRow row = latest.get(holding.getAsset().getSymbol());
            double value = (row != null)
                    ? row.close().doubleValue() * holding.getShares()
                    : holding.cost().doubleValue();
            values.put(holding.getAsset().getSymbol(), value);
        }
        return values;
    }

    private ReoptimizationResponse toResponse(Reoptimization reoptimization) {
        String changesJson = reoptimization.getChangesJson();
        List<WeightChange> changes = (changesJson == null) ? List.of() : jsonMapper.readValue(changesJson, CHANGE_LIST);
        return ReoptimizationResponse.of(reoptimization, changes);
    }
}
