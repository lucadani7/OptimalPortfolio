package com.lucadani.backend.web;

import com.lucadani.backend.marketdata.PriceImportJob;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
@Tag(name = "Market data", description = "Import prices from the configured market data provider")
public class MarketDataController {

    private final JobScheduler jobScheduler;
    private final PriceImportJob priceImportJob;

    public record ImportStartedResponse(String jobId) {
    }

    /** Starts a price import now, in the background. Its progress is visible in the JobRunr dashboard. */
    @PostMapping("/import")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportStartedResponse importNow() {
        JobId jobId = jobScheduler.enqueue(() -> priceImportJob.importLatestPrices(JobContext.Null));
        return new ImportStartedResponse(jobId.toString());
    }
}
