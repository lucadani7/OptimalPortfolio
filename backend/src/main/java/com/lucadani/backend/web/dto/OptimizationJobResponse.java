package com.lucadani.backend.web.dto;

import com.lucadani.backend.model.OptimizationJob;
import com.lucadani.backend.model.OptimizationJobKind;
import com.lucadani.backend.model.OptimizationJobStatus;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * An optimization job and, once it has succeeded, its result. {@code progress} is a percentage.
 * {@code error} is set when the job failed; {@code frontier} when a FRONTIER job succeeded.
 */
public record OptimizationJobResponse(
        UUID id,
        OptimizationJobKind kind,
        OptimizationJobStatus status,
        int progress,
        @Nullable String error,
        @Nullable FrontierResponse frontier,
        Instant createdAt,
        @Nullable Instant startedAt,
        @Nullable Instant finishedAt
) {

    public static OptimizationJobResponse of(OptimizationJob job, @Nullable FrontierResponse frontier) {
        return new OptimizationJobResponse(job.getId(), job.getKind(), job.getStatus(), job.getProgress(),
                job.getError(), frontier, job.getCreatedAt(), job.getStartedAt(), job.getFinishedAt());
    }
}
