package com.lucadani.backend.web;

import com.lucadani.backend.jobs.OptimizationJobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Server-sent event streams of optimization jobs. Each stream starts with the job's current state,
 * then gets every change as a "job" event, and closes after the job has finished.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationJobStreams {

    public static final String EVENT_NAME = "job";

    /** Streams are closed after this long; browsers reconnect on their own and get the current state. */
    static final Duration TIMEOUT = Duration.ofMinutes(10);

    private final Map<UUID, Set<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private final JsonMapper jsonMapper;

    /**
     * Opens a stream for a job. The subscriber is registered before the current state is read,
     * so a change made in between is never missed (it may arrive twice, which is harmless).
     */
    public SseEmitter subscribe(UUID jobId, Supplier<OptimizationJobEvent> currentState) {
        SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
        subscribers.compute(jobId, (id, emitters) -> {
            Set<SseEmitter> set = (emitters != null) ? emitters : ConcurrentHashMap.newKeySet();
            set.add(emitter);
            return set;
        });
        emitter.onCompletion(() -> unsubscribe(jobId, emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> unsubscribe(jobId, emitter));

        deliver(jobId, emitter, currentState.get());
        return emitter;
    }

    /** Runs after the change is committed, or right away for changes made outside a transaction. */
    @TransactionalEventListener(fallbackExecution = true)
    public void onJobEvent(OptimizationJobEvent event) {
        Set<SseEmitter> emitters = subscribers.get(event.jobId());
        if (emitters != null) {
            emitters.forEach(emitter -> deliver(event.jobId(), emitter, event));
        }
    }

    private void deliver(UUID jobId, SseEmitter emitter, OptimizationJobEvent event) {
        try {
            // Serialized here, so the stream does not depend on which message converters MVC has
            emitter.send(SseEmitter.event().name(EVENT_NAME).data(jsonMapper.writeValueAsString(event)));
            if (event.status().isFinished()) {
                emitter.complete();
            }
        } catch (IOException | IllegalStateException e) {
            // The client went away, or the stream was already closed
            log.debug("Could not send {} to a subscriber of job {}", event.status(), jobId, e);
            unsubscribe(jobId, emitter);
        }
    }

    private void unsubscribe(UUID jobId, SseEmitter emitter) {
        subscribers.computeIfPresent(jobId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
