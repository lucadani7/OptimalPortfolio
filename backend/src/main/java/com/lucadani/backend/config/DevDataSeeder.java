package com.lucadani.backend.config;

import com.lucadani.backend.marketdata.PriceModel;
import com.lucadani.backend.marketdata.TradingCalendar;
import com.lucadani.backend.model.Asset;
import com.lucadani.backend.model.Bond;
import com.lucadani.backend.model.Etf;
import com.lucadani.backend.model.Stock;
import com.lucadani.backend.repository.AssetRepository;
import com.lucadani.backend.repository.PricePointRepository;
import com.lucadani.backend.service.PriceService;
import com.lucadani.backend.web.dto.PriceEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds sample assets and synthetic price history for local development.
 * Each asset follows its own {@link PriceModel}, driven partly by a shared market factor
 * so that assets are realistically correlated. The data is deterministic (fixed seeds).
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final int YEARS_OF_HISTORY = 3;
    private static final long MARKET_SEED = 42L;

    private final AssetRepository assetRepository;
    private final PricePointRepository pricePointRepository;
    private final PriceService priceService;

    /**
     * @param marketCorrelation correlation of the asset's daily shocks with the market factor, in [-1, 1]
     */
    private record SampleAsset(Asset asset, double startPrice, PriceModel model, double marketCorrelation) {
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDate end = LocalDate.now().minusDays(1);
        List<LocalDate> tradingDays = TradingCalendar.tradingDays(end.minusYears(YEARS_OF_HISTORY), end);
        double[] marketShocks = gaussianSeries(new Random(MARKET_SEED), tradingDays.size());

        for (SampleAsset sample : sampleAssets()) {
            Asset asset = assetRepository.findBySymbol(sample.asset().getSymbol())
                    .orElseGet(() -> assetRepository.save(sample.asset()));

            if (pricePointRepository.existsByAsset(asset)) {
                continue;
            }

            List<PriceEntry> prices = simulatePrices(sample, tradingDays, marketShocks);
            priceService.importPrices(asset.getSymbol(), prices);
            log.info("Seeded {} prices for {}", prices.size(), asset.getSymbol());
        }
    }

    private static List<SampleAsset> sampleAssets() {
        return List.of(
                new SampleAsset(new Stock("AAPL", "Apple Inc.", "Technology", "USD", "NASDAQ"),
                        180, PriceModel.geometricBrownian(0.12, 0.28), 0.70),
                new SampleAsset(new Stock("MSFT", "Microsoft Corp.", "Technology", "USD", "NASDAQ"),
                        380, PriceModel.geometricBrownian(0.11, 0.25), 0.75),
                new SampleAsset(new Stock("JNJ", "Johnson & Johnson", "Healthcare", "USD", "NYSE"),
                        155, PriceModel.geometricBrownian(0.06, 0.16), 0.40),
                new SampleAsset(new Stock("XOM", "Exxon Mobil Corp.", "Energy", "USD", "NYSE"),
                        105, PriceModel.geometricBrownian(0.07, 0.27), 0.45),
                new SampleAsset(new Etf("VOO", "Vanguard S&P 500 ETF", "Broad Market", "USD",
                        new BigDecimal("0.00030")),
                        450, PriceModel.geometricBrownian(0.09, 0.17), 0.95),
                new SampleAsset(new Etf("GLD", "SPDR Gold Shares", "Commodities", "USD",
                        new BigDecimal("0.00400")),
                        190, PriceModel.geometricBrownian(0.05, 0.15), 0.05),
                new SampleAsset(new Etf("BND", "Vanguard Total Bond Market ETF", "Bonds", "USD",
                        new BigDecimal("0.00030")),
                        72, PriceModel.meanReverting(72, 1.5, 0.06), -0.10),
                new SampleAsset(new Bond("US10Y", "US Treasury 10 Year", "Government Bonds", "USD",
                        new BigDecimal("0.04250"), LocalDate.of(2035, 11, 15)),
                        98, PriceModel.meanReverting(100, 2.0, 0.07), -0.20)
        );
    }

    private static List<PriceEntry> simulatePrices(SampleAsset sample, List<LocalDate> days, double[] marketShocks) {
        Random random = new Random(sample.asset().getSymbol().hashCode());
        double rho = sample.marketCorrelation();
        double idiosyncraticWeight = Math.sqrt(1 - rho * rho);

        // One combined shock per day after the first one
        double[] shocks = new double[days.size() - 1];
        for (int i = 0; i < shocks.length; i++) {
            shocks[i] = rho * marketShocks[i + 1] + idiosyncraticWeight * random.nextGaussian();
        }

        double[] path = sample.model().path(sample.startPrice(), shocks);

        List<PriceEntry> prices = new ArrayList<>(days.size());
        for (int i = 0; i < days.size(); i++) {
            prices.add(new PriceEntry(days.get(i), BigDecimal.valueOf(path[i]).setScale(6, RoundingMode.HALF_UP)));
        }
        return prices;
    }

    private static double[] gaussianSeries(Random random, int size) {
        double[] values = new double[size];
        for (int i = 0; i < size; i++) {
            values[i] = random.nextGaussian();
        }
        return values;
    }
}
