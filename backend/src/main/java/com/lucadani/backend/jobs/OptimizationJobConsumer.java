package com.lucadani.backend.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Runs the jobs that arrive on the jobs queue. The message is acknowledged once the job has finished,
 * so a job whose instance stops halfway is delivered again and restarted.
 */
@Component
@ConditionalOnProperty(name = "app.jobs.consumer.enabled", havingValue = "true")
@RequiredArgsConstructor
public class OptimizationJobConsumer {

    private final OptimizationJobRunner runner;

    @RabbitListener(queues = JobMessagingConfig.JOBS_QUEUE)
    public void onJob(String optimizationJobId) {
        runner.run(UUID.fromString(optimizationJobId));
    }
}
