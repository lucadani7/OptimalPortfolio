package com.lucadani.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An optimization that runs in the background. It moves from QUEUED to RUNNING, then ends
 * as SUCCEEDED (with a result) or FAILED (with an error). Request and result are stored as JSON.
 */
@Entity
@Table(name = "optimization_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OptimizationJob {

    /** Longest error message kept, matching the column. */
    public static final int MAX_ERROR_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OptimizationJobKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OptimizationJobStatus status;

    /** Percentage done, 0 to 100. */
    @Column(nullable = false)
    private int progress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String requestJson;

    @JdbcTypeCode(SqlTypes.JSON)
    private @Nullable String resultJson;

    @Column(length = MAX_ERROR_LENGTH)
    private @Nullable String error;

    @Column(nullable = false)
    private Instant createdAt;

    private @Nullable Instant startedAt;

    private @Nullable Instant finishedAt;

    @Version
    private long version;

    public static OptimizationJob queued(OptimizationJobKind kind, String requestJson, Instant now) {
        OptimizationJob job = new OptimizationJob();
        job.kind = Objects.requireNonNull(kind);
        job.requestJson = Objects.requireNonNull(requestJson);
        job.status = OptimizationJobStatus.QUEUED;
        job.createdAt = now;
        return job;
    }

    /** Marks the job as running. Allowed again on a running job, which happens when it is restarted. */
    public void start(Instant now) {
        requireUnfinished();
        status = OptimizationJobStatus.RUNNING;
        startedAt = now;
    }

    public void succeed(String resultJson, Instant now) {
        requireUnfinished();
        this.resultJson = Objects.requireNonNull(resultJson);
        status = OptimizationJobStatus.SUCCEEDED;
        progress = 100;
        finishedAt = now;
    }

    public void fail(String error, Instant now) {
        requireUnfinished();
        this.error = (error.length() > MAX_ERROR_LENGTH) ? error.substring(0, MAX_ERROR_LENGTH) : error;
        status = OptimizationJobStatus.FAILED;
        finishedAt = now;
    }

    private void requireUnfinished() {
        if (status.isFinished()) {
            throw new IllegalStateException("Optimization job " + id + " has already finished as " + status);
        }
    }
}
