package com.lucadani.backend.jobs;

import com.lucadani.backend.exception.ResourceNotFoundException;
import com.lucadani.backend.model.OptimizationJob;
import com.lucadani.backend.model.OptimizationJobKind;
import com.lucadani.backend.model.OptimizationJobStatus;
import com.lucadani.backend.repository.OptimizationJobRepository;
import com.lucadani.backend.service.StatisticsQuery;
import com.lucadani.backend.web.dto.FrontierRequest;
import com.lucadani.backend.web.dto.FrontierResponse;
import com.lucadani.backend.web.dto.OptimizationJobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Submits optimization jobs and records how they move through their states. Every change is published
 * as an {@link OptimizationJobEvent}; listeners receive it once the change is committed.
 */
@Service
@RequiredArgsConstructor
public class OptimizationJobService {

    private final OptimizationJobRepository repository;
    private final JobDispatcher dispatcher;
    private final ApplicationEventPublisher events;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    /**
     * Saves a job that traces the efficient frontier, then dispatches it. Not transactional on purpose:
     * the job is committed before it is dispatched, so whatever runs it can always find it.
     */
    public OptimizationJob submitFrontier(FrontierRequest request) {
        // Fail fast, with a 400, on input that the job would reject anyway
        StatisticsQuery.of(request.symbols(), request.from(), request.to());
        request.domainConstraints();

        OptimizationJob job = repository.save(OptimizationJob.queued(
                OptimizationJobKind.FRONTIER, jsonMapper.writeValueAsString(request), clock.instant()));
        dispatcher.dispatch(job.getId());
        return job;
    }

    @Transactional(readOnly = true)
    public OptimizationJob get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Optimization job", id));
    }

    @Transactional(readOnly = true)
    public OptimizationJobEvent currentState(UUID id) {
        return OptimizationJobEvent.of(get(id));
    }

    public OptimizationJobResponse toResponse(OptimizationJob job) {
        String result = job.getResultJson();
        FrontierResponse frontier = (job.getKind() == OptimizationJobKind.FRONTIER && result != null)
                ? jsonMapper.readValue(result, FrontierResponse.class)
                : null;
        return OptimizationJobResponse.of(job, frontier);
    }

    @Transactional
    public OptimizationJob start(UUID id) {
        OptimizationJob job = get(id);
        job.start(clock.instant());
        events.publishEvent(OptimizationJobEvent.of(job));
        return job;
    }

    /**
     * Records progress of a running job. Lower values than the current one are ignored.
     * Progress is reported from inside the optimization, which may run in a read-only transaction;
     * that transaction is suspended, so the update commits on its own and the event goes out right away.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void reportProgress(UUID id, int progress) {
        if (repository.raiseProgress(id, progress) > 0) {
            events.publishEvent(OptimizationJobEvent.progress(id, progress));
        }
    }

    @Transactional
    public void succeed(UUID id, String resultJson) {
        OptimizationJob job = get(id);
        job.succeed(resultJson, clock.instant());
        events.publishEvent(OptimizationJobEvent.of(job));
    }

    @Transactional
    public void fail(UUID id, String error) {
        OptimizationJob job = get(id);
        job.fail(error, clock.instant());
        events.publishEvent(OptimizationJobEvent.of(job));
    }

    /**
     * Fails the jobs created before {@code before} that never finished, e.g. because the application
     * stopped while they were running.
     *
     * @return how many jobs were failed
     */
    @Transactional
    public int failUnfinishedCreatedBefore(Instant before) {
        List<OptimizationJob> stale = repository.findAllByStatusInAndCreatedAtBefore(
                EnumSet.of(OptimizationJobStatus.QUEUED, OptimizationJobStatus.RUNNING), before);
        Instant now = clock.instant();
        for (OptimizationJob job : stale) {
            job.fail("The job was interrupted before it could finish. Start it again.", now);
            events.publishEvent(OptimizationJobEvent.of(job));
        }
        return stale.size();
    }
}
