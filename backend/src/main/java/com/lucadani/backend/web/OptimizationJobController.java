package com.lucadani.backend.web;

import com.lucadani.backend.jobs.OptimizationJobService;
import com.lucadani.backend.model.OptimizationJob;
import com.lucadani.backend.web.dto.FrontierRequest;
import com.lucadani.backend.web.dto.OptimizationJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/optimization/jobs")
@RequiredArgsConstructor
@Tag(name = "Optimization jobs", description = "Run optimizations in the background and follow their progress")
public class OptimizationJobController {

    private final OptimizationJobService jobService;
    private final OptimizationJobStreams streams;

    @Operation(summary = "Start tracing the efficient frontier in the background")
    @PostMapping("/frontier")
    public ResponseEntity<OptimizationJobResponse> traceFrontier(@Valid @RequestBody FrontierRequest request,
                                                                 UriComponentsBuilder uriBuilder) {
        OptimizationJob job = jobService.submitFrontier(request);
        URI location = uriBuilder.path("/api/optimization/jobs/{id}").buildAndExpand(job.getId()).toUri();
        return ResponseEntity.accepted().location(location).body(jobService.toResponse(job));
    }

    @Operation(summary = "Get a job, with its result once it has succeeded")
    @GetMapping("/{id}")
    public OptimizationJobResponse get(@PathVariable UUID id) {
        return jobService.toResponse(jobService.get(id));
    }

    @Operation(summary = "Follow a job as server-sent events",
            description = "Each \"job\" event holds jobId, status, progress (a percentage) and error. "
                    + "The stream starts with the current state and closes once the job has finished.")
    @GetMapping(path = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable UUID id) {
        jobService.get(id); // 404 before the stream opens
        return streams.subscribe(id, () -> jobService.currentState(id));
    }
}
