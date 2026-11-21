package com.lucadani.backend.web;

import com.lucadani.backend.jobs.JobMessagingConfig;
import com.lucadani.backend.jobs.OptimizationJobEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Passes job events from other instances (usually workers) on to this instance's SSE subscribers. */
@Component
@ConditionalOnWebApplication
@RequiredArgsConstructor
public class OptimizationJobEventListener {

    private final OptimizationJobStreams streams;
    private final JsonMapper jsonMapper;

    @RabbitListener(queues = "#{jobEventsQueue.name}")
    public void onEvent(Message message) {
        Object origin = message.getMessageProperties().getHeader(JobMessagingConfig.ORIGIN_HEADER);
        if (JobMessagingConfig.INSTANCE_ID.equals(origin)) {
            return; // Sent by this instance, whose subscribers already got it directly
        }
        streams.onJobEvent(jsonMapper.readValue(message.getBody(), OptimizationJobEvent.class));
    }
}
