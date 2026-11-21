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
import java.util.function.ToIntFunction;

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

    /** How much history to fetch for an asset that has no prices yet, or when reloading. */
    static final int HISTORY_YEARS = 3;

    private final AssetRepository assetRepository;
    private final PricePointRepository pricePointRepository;
    private final PriceService priceService;
    private final MarketDataProvider marketDataProvider;
    private final Clock clock;

    @Recurring(id = RECURRING_ID, cron = "${app.market-data.import-cron}", zoneId = "${app.market-data.zone-id}")
    @Job(name = "Import the latest prices", retries = 3)
    public void importLatestPrices(JobContext jobContext) {
        LocalDate today = LocalDate.now(clock);
        forEachAsset(jobContext, "Imported", asset -> importMissing(asset, today));
    }

    /**
     * Replaces every asset's history with the provider's last {@value #HISTORY_YEARS} years: after switching
     * providers, and now and then with a real one, as its adjusted closes change after dividends and splits.
     */
    @Job(name = "Reload the full price history", retries = 1)
    public void reloadHistory(JobContext jobContext) {
        LocalDate today = LocalDate.now(clock);
        forEachAsset(jobContext, "Reloaded", asset -> {
            List<PriceEntry> prices = marketDataProvider.fetchPrices(asset, today.minusYears(HISTORY_YEARS), today);
            // An empty answer is more likely a provider problem than a real lack of prices: keep what is there
            if (prices.isEmpty()) {
                throw new IllegalStateException("The provider returned no prices for " + asset.getSymbol());
            }
            priceService.replacePrices(asset.getSymbol(), prices);
            return prices.size();
        });
    }

    /**
     * Runs {@code work} for every asset, reporting progress. One failing asset does not stop the others;
     * the job fails at the end so that JobRunr retries it.
     */
    private void forEachAsset(JobContext jobContext, String verb, ToIntFunction<Asset> work) {
        List<Asset> assets = assetRepository.findAllByOrderBySymbolAsc();
        JobDashboardProgressBar progress = jobContext.progressBar(assets.size());

        int imported = 0;
        List<String> failed = new ArrayList<>();
        for (Asset asset : assets) {
            try {
                imported += work.applyAsInt(asset);
                progress.incrementSucceeded();
            } catch (RuntimeException e) {
                log.warn("Could not import prices for {}", asset.getSymbol(), e);
                failed.add(asset.getSymbol());
                progress.incrementFailed();
            }
        }

        String summary = "%s %d prices for %d assets from the %s provider"
                .formatted(verb, imported, assets.size() - failed.size(), marketDataProvider.name());
        log.info(summary);
        jobContext.logger().info(summary);
        if (!failed.isEmpty()) {
            throw new IllegalStateException("Price import failed for " + String.join(", ", failed));
        }
    }

    private int importMissing(Asset asset, LocalDate today) {
        LocalDate from = pricePointRepository.findFirstByAssetOrderByTradeDateDesc(asset)
                .map(last -> last.getTradeDate().plusDays(1))
                .orElse(today.minusYears(HISTORY_YEARS));
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
