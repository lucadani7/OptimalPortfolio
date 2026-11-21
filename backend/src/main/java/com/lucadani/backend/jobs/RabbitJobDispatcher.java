package com.lucadani.backend.jobs;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Sends optimization jobs through RabbitMQ to whichever instance consumes them (persistent messages). */
@Component
@ConditionalOnProperty(name = "app.jobs.dispatcher", havingValue = "rabbit", matchIfMissing = true)
@RequiredArgsConstructor
public class RabbitJobDispatcher implements JobDispatcher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void dispatch(UUID optimizationJobId) {
        rabbitTemplate.convertAndSend(JobMessagingConfig.JOBS_EXCHANGE, JobMessagingConfig.JOBS_QUEUE,
                optimizationJobId.toString());
    }
}
