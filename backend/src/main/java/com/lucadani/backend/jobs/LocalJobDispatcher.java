package com.lucadani.backend.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Runs optimization jobs in this application, right away, on the application task executor
 * (virtual threads, as spring.threads.virtual.enabled is on). JobRunr is not used here: it looks
 * for new jobs only every few seconds, too slow for a user waiting for a chart. A job lost when the
 * application stops is marked as interrupted by {@link OptimizationJobMaintenance}.
 */
@Component
@RequiredArgsConstructor
public class LocalJobDispatcher implements JobDispatcher {

    private final TaskExecutor applicationTaskExecutor;
    // Looked up on use: the runner depends on OptimizationJobService, which depends on this dispatcher
    private final ObjectProvider<OptimizationJobRunner> runner;

    @Override
    public void dispatch(UUID optimizationJobId) {
        applicationTaskExecutor.execute(() -> runner.getObject().run(optimizationJobId));
    }
}
