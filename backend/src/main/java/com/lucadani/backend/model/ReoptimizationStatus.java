package com.lucadani.backend.model;

public enum ReoptimizationStatus {
    SUCCEEDED,
    /** The strategy could not be run, e.g. no portfolio satisfies the rules with the newer prices. */
    FAILED
}
