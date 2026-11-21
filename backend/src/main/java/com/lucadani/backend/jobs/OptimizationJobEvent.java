package com.lucadani.backend.jobs;

import com.lucadani.backend.model.OptimizationJob;
import com.lucadani.backend.model.OptimizationJobStatus;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** A change in an optimization job: it started, made progress or finished. Sent to SSE subscribers as JSON. */
public record OptimizationJobEvent(UUID jobId, OptimizationJobStatus status, int progress, @Nullable String error) {

    public static OptimizationJobEvent of(OptimizationJob job) {
        return new OptimizationJobEvent(job.getId(), job.getStatus(), job.getProgress(), job.getError());
    }

    public static OptimizationJobEvent progress(UUID jobId, int progress) {
        return new OptimizationJobEvent(jobId, OptimizationJobStatus.RUNNING, progress, null);
    }
}
