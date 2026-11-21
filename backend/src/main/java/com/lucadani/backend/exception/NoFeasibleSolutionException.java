package com.lucadani.backend.exception;

/** Thrown when no portfolio satisfies all the constraints of a problem. */
public class NoFeasibleSolutionException extends RuntimeException {

    public NoFeasibleSolutionException(String message) {
        super(message);
    }
}
