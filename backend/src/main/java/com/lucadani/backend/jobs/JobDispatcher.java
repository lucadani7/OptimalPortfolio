package com.lucadani.backend.jobs;

import java.util.UUID;

/**
 * Hands a saved optimization job to whatever runs it. Running it in this application for now;
 * a RabbitMQ-based dispatcher will send it to a separate worker instead.
 */
public interface JobDispatcher {

    void dispatch(UUID optimizationJobId);
}
