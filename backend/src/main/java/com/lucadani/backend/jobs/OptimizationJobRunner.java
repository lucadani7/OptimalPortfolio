package com.lucadani.backend.jobs;

import com.lucadani.backend.exception.NoFeasibleSolutionException;
import com.lucadani.backend.exception.ResourceNotFoundException;
import com.lucadani.backend.model.OptimizationJob;
import com.lucadani.backend.optimization.ProgressListener;
import com.lucadani.backend.service.FrontierRun;
import com.lucadani.backend.service.OptimizationService;
import com.lucadani.backend.service.StatisticsQuery;
import com.lucadani.backend.web.dto.FrontierRequest;
import com.lucadani.backend.web.dto.FrontierResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Runs one optimization job from start to finish, reporting its progress along the way. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationJobRunner {

    private final OptimizationJobService jobService;
    private final OptimizationService optimizationService;
    private final JsonMapper jsonMapper;

    /** Never throws once the job has started: every failure is recorded on the job. */
    public void run(UUID jobId) {
        if (jobService.get(jobId).getStatus().isFinished()) {
            // A message delivered twice, e.g. after a worker restart: the result is already there
            log.info("Optimization job {} has already finished, skipping it", jobId);
            return;
        }
        OptimizationJob job = jobService.start(jobId);
        try {
            String resultJson = switch (job.getKind()) {
                case FRONTIER -> traceFrontier(job);
            };
            jobService.succeed(jobId, resultJson);
        } catch (NoFeasibleSolutionException | IllegalArgumentException | ResourceNotFoundException e) {
            // The request cannot be solved: a normal outcome for the user, not an error of the application
            jobService.fail(jobId, messageOf(e));
        } catch (RuntimeException e) {
            log.error("Optimization job {} failed", jobId, e);
            jobService.fail(jobId, "Unexpected error: " + messageOf(e));
        }
    }

    private String traceFrontier(OptimizationJob job) {
        FrontierRequest request = jsonMapper.readValue(job.getRequestJson(), FrontierRequest.class);
        StatisticsQuery query = StatisticsQuery.of(request.symbols(), request.from(), request.to());
        FrontierRun run = optimizationService.frontier(query, request.domainConstraints(),
                request.riskFreeRateOrZero(), request.pointsOrDefault(), percentReporter(job.getId()));
        return jsonMapper.writeValueAsString(FrontierResponse.of(run));
    }

    /** Turns step counts into whole percentages and reports each new, higher percentage once. */
    private ProgressListener percentReporter(UUID jobId) {
        AtomicInteger reported = new AtomicInteger();
        return (completed, total) -> {
            // 100% comes with the result, so progress stays below it while computing
            int percent = (int) Math.min(99, 100L * completed / Math.max(total, 1));
            if (reported.getAndAccumulate(percent, Math::max) < percent) {
                jobService.reportProgress(jobId, percent);
            }
        };
    }

    private static String messageOf(RuntimeException e) {
        return Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());
    }
}
