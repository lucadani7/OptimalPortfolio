package com.lucadani.backend.model;

/** What an optimization job computes. */
public enum OptimizationJobKind {
    /** Traces the efficient frontier; the request is a FrontierRequest, the result a FrontierResponse. */
    FRONTIER
}
