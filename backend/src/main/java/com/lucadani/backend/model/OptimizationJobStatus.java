package com.lucadani.backend.model;

public enum OptimizationJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED;

    /** True once the job has a result or an error; it will not change again. */
    public boolean isFinished() {
        return this == SUCCEEDED || this == FAILED;
    }
}
