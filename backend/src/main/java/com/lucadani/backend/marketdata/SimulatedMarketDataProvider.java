package com.lucadani.backend.marketdata;

import com.lucadani.backend.model.AssetType;
import com.lucadani.backend.model.Asset;
import com.lucadani.backend.repository.PricePointRepository;
import com.lucadani.backend.web.dto.PriceEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Continues each asset's price history with simulated closes, so the app has fresh data without
 * a market data account. Each day combines a shared market shock with a shock of the asset itself,
 * both seeded by the date (and the symbol), so the same day always gets the same price.
 * SplittableRandom mixes its seed well, so consecutive days get independent shocks
 * (java.util.Random would give correlated first values for consecutive seeds).
 */
@Component
@ConditionalOnProperty(name = "app.market-data.provider", havingValue = "simulated", matchIfMissing = true)
@RequiredArgsConstructor
public class SimulatedMarketDataProvider implements MarketDataProvider {

    /** Starting price for an asset that has no history yet. */
    static final double DEFAULT_START_PRICE = 100;

    private final PricePointRepository pricePointRepository;

    @Override
    public String name() {
        return "simulated";
    }

    @Override
    public List<PriceEntry> fetchPrices(Asset asset, LocalDate from, LocalDate to) {
        double lastClose = pricePointRepository.findFirstByAssetAndTradeDateBeforeOrderByTradeDateDesc(asset, from)
                .map(point -> point.getClose().doubleValue())
                .orElse(DEFAULT_START_PRICE);
        return simulate(asset.getSymbol(), asset.getType(), lastClose, TradingCalendar.tradingDays(from, to));
    }

    /** Pure and deterministic: the same symbol, type, start price and days always give the same prices. */
    static List<PriceEntry> simulate(String symbol, AssetType type, double startPrice, List<LocalDate> days) {
        Behaviour behaviour = Behaviour.of(type, startPrice);
        double idiosyncraticWeight = Math.sqrt(1 - behaviour.marketCorrelation() * behaviour.marketCorrelation());

        List<PriceEntry> prices = new ArrayList<>(days.size());
        double price = startPrice;
        for (LocalDate day : days) {
            double market = new SplittableRandom(day.toEpochDay()).nextGaussian();
            double own = new SplittableRandom(31L * symbol.hashCode() + day.toEpochDay()).nextGaussian();
            double shock = behaviour.marketCorrelation() * market + idiosyncraticWeight * own;
            price = behaviour.model().next(price, shock);
            prices.add(new PriceEntry(day, BigDecimal.valueOf(price).setScale(6, RoundingMode.HALF_UP)));
        }
        return prices;
    }

    /** Typical dynamics of each kind of asset. */
    private record Behaviour(PriceModel model, double marketCorrelation) {

        static Behaviour of(AssetType type, double startPrice) {
            return switch (type) {
                case STOCK -> new Behaviour(PriceModel.geometricBrownian(0.08, 0.25), 0.6);
                case ETF -> new Behaviour(PriceModel.geometricBrownian(0.06, 0.15), 0.8);
                case BOND -> new Behaviour(PriceModel.meanReverting(startPrice, 1.5, 0.06), -0.1);
            };
        }
    }
}
