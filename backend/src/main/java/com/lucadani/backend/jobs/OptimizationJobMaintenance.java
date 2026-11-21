package com.lucadani.backend.jobs;

import com.lucadani.backend.repository.OptimizationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Keeps the optimization_jobs table tidy: fails jobs that got stuck and deletes old ones. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationJobMaintenance {

    /** No optimization takes this long; a job still unfinished after it was interrupted. */
    static final Duration MAX_RUN_TIME = Duration.ofMinutes(30);

    /** Finished jobs are kept this long, so their results can still be fetched. */
    static final Duration RETENTION = Duration.ofDays(7);

    private final OptimizationJobService jobService;
    private final OptimizationJobRepository repository;
    private final Clock clock;

    @Recurring(id = "optimization-job-maintenance", cron = "15 * * * *")
    @Job(name = "Clean up optimization jobs")
    public void cleanUp() {
        Instant now = clock.instant();
        int interrupted = jobService.failUnfinishedCreatedBefore(now.minus(MAX_RUN_TIME));
        int deleted = repository.deleteCreatedBefore(now.minus(RETENTION));
        if (interrupted > 0 || deleted > 0) {
            log.info("Optimization jobs: {} marked as interrupted, {} deleted", interrupted, deleted);
        }
    }
}
