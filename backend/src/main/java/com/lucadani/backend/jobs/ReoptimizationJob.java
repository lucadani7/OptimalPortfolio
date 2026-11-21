package com.lucadani.backend.jobs;

import com.lucadani.backend.exception.ResourceNotFoundException;
import com.lucadani.backend.service.ReoptimizationService;
import com.lucadani.backend.web.dto.ReoptimizationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

/** The scheduled reoptimization of one portfolio; JobRunr runs it as a recurring job per portfolio. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReoptimizationJob {

    private final ReoptimizationService reoptimizationService;

    public static String recurringJobId(Long portfolioId) {
        return "reoptimize-portfolio-" + portfolioId;
    }

    @Job(name = "Reoptimize portfolio %0", retries = 2)
    public void run(Long portfolioId) {
        try {
            ReoptimizationResponse result = reoptimizationService.reoptimize(portfolioId);
            log.info("Reoptimized portfolio {}: {}", portfolioId, result.status());
        } catch (ResourceNotFoundException e) {
            // Deleted after the run was planned; deleting a portfolio also removes its recurring job
            log.info("Portfolio {} no longer exists, skipping its reoptimization", portfolioId);
        }
    }
}
