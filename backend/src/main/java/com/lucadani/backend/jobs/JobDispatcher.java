package com.lucadani.backend.jobs;

import java.util.UUID;

/**
 * Hands a saved optimization job to whatever runs it: RabbitMQ by default, or this instance
 * (see {@code app.jobs.dispatcher}).
 */
public interface JobDispatcher {

    void dispatch(UUID optimizationJobId);
}
