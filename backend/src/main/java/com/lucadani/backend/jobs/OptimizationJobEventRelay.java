package com.lucadani.backend.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * On an instance that runs jobs, forwards every job event to RabbitMQ, so the web instances can pass
 * it on to their SSE subscribers. Like local listeners, it sends only committed changes.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.jobs.consumer.enabled", havingValue = "true")
@RequiredArgsConstructor
public class OptimizationJobEventRelay {

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    @TransactionalEventListener(fallbackExecution = true)
    public void relay(OptimizationJobEvent event) {
        try {
            rabbitTemplate.convertAndSend(JobMessagingConfig.EVENTS_EXCHANGE, "", jsonMapper.writeValueAsString(event),
                    message -> {
                        message.getMessageProperties().setHeader(JobMessagingConfig.ORIGIN_HEADER,
                                JobMessagingConfig.INSTANCE_ID);
                        return message;
                    });
        } catch (AmqpException e) {
            // Progress is best effort: the job goes on, and clients still get the final state from the API
            log.warn("Could not relay {} of job {}", event.status(), event.jobId(), e);
        }
    }
}
