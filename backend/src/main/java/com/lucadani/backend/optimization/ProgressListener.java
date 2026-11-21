package com.lucadani.backend.optimization;

/**
 * Receives progress updates from a long computation. May be called from several threads at once,
 * and updates may arrive out of order, so implementations must be thread-safe.
 */
@FunctionalInterface
public interface ProgressListener {

    /** Ignores every update. */
    ProgressListener NONE = (completed, total) -> {
    };

    /** {@code completed} of {@code total} steps are done. */
    void onProgress(int completed, int total);
}
