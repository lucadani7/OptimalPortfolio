package com.lucadani.backend.exception;

/** Thrown when a saved job could not be handed over to be run, e.g. because the broker is down. */
public class JobDispatchException extends RuntimeException {

    public JobDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
