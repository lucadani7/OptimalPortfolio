package com.lucadani.backend.marketdata;

import com.lucadani.backend.model.Asset;
import com.lucadani.backend.repository.AssetRepository;
import com.lucadani.backend.repository.PricePointRepository;
import com.lucadani.backend.service.PriceService;
import com.lucadani.backend.web.dto.PriceEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobDashboardProgressBar;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports the prices each asset is missing, from the day after its last known price up to today.
 * Runs on a schedule (see {@code app.market-data.import-cron}) and can also be started on demand.
 * Importing is idempotent, so a retry or an extra run never duplicates prices.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceImportJob {

    public static final String RECURRING_ID = "daily-price-import";

    /** How much history to fetch for an asset that has no prices yet. */
    static final int INITIAL_HISTORY_YEARS = 1;

    private final AssetRepository assetRepository;
    private final PricePointRepository pricePointRepository;
    private final PriceService priceService;
    private final MarketDataProvider marketDataProvider;
    private final Clock clock;

    @Recurring(id = RECURRING_ID, cron = "${app.market-data.import-cron}", zoneId = "${app.market-data.zone-id}")
    @Job(name = "Import the latest prices", retries = 3)
    public void importLatestPrices(JobContext jobContext) {
        LocalDate today = LocalDate.now(clock);
        List<Asset> assets = assetRepository.findAllByOrderBySymbolAsc();
        JobDashboardProgressBar progress = jobContext.progressBar(assets.size());

        int imported = 0;
        List<String> failed = new ArrayList<>();
        for (Asset asset : assets) {
            try {
                imported += importAsset(asset, today);
                progress.incrementSucceeded();
            } catch (RuntimeException e) {
                // One failing asset must not stop the others; the job fails at the end so JobRunr retries it
                log.warn("Could not import prices for {}", asset.getSymbol(), e);
                failed.add(asset.getSymbol());
                progress.incrementFailed();
            }
        }

        String summary = "Imported %d prices for %d assets from the %s provider"
                .formatted(imported, assets.size() - failed.size(), marketDataProvider.name());
        log.info(summary);
        jobContext.logger().info(summary);
        if (!failed.isEmpty()) {
            throw new IllegalStateException("Price import failed for " + String.join(", ", failed));
        }
    }

    private int importAsset(Asset asset, LocalDate today) {
        LocalDate from = pricePointRepository.findFirstByAssetOrderByTradeDateDesc(asset)
                .map(last -> last.getTradeDate().plusDays(1))
                .orElse(today.minusYears(INITIAL_HISTORY_YEARS));
        if (from.isAfter(today)) {
            return 0;
        }
        List<PriceEntry> prices = marketDataProvider.fetchPrices(asset, from, today);
        if (!prices.isEmpty()) {
            priceService.importPrices(asset.getSymbol(), prices);
        }
        return prices.size();
    }
}
