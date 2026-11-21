package com.lucadani.backend.service;

import com.lucadani.backend.analytics.MarketStatistics;
import com.lucadani.backend.analytics.PriceSeries;
import com.lucadani.backend.analytics.StatisticsCalculator;
import com.lucadani.backend.exception.ResourceNotFoundException;
import com.lucadani.backend.model.Asset;
import com.lucadani.backend.repository.AssetRepository;
import com.lucadani.backend.repository.PricePointRepository;
import com.lucadani.backend.repository.PriceRow;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    public static final String CACHE_NAME = "statistics";

    private final AssetRepository assetRepository;
    private final PricePointRepository pricePointRepository;
    private final StatisticsCalculator calculator;

    /**
     * Annualized statistics for the requested assets over the requested period.
     * Results are cached; the cache is cleared whenever prices are imported or an asset is deleted.
     */
    @Cacheable(cacheNames = CACHE_NAME)
    public MarketStatistics getStatistics(StatisticsQuery query) {
        requireAllAssetsExist(query.symbols());

        // Keep the requested order of symbols in the result
        Map<String, NavigableMap<LocalDate, Double>> closesBySymbol = new LinkedHashMap<>();
        query.symbols().forEach(symbol -> closesBySymbol.put(symbol, new TreeMap<>()));

        for (PriceRow row : pricePointRepository.findRows(query.symbols(), query.from(), query.to())) {
            closesBySymbol.get(row.symbol()).put(row.tradeDate(), row.close().doubleValue());
        }

        List<PriceSeries> series = closesBySymbol.entrySet().stream()
                .map(entry -> new PriceSeries(entry.getKey(), entry.getValue()))
                .toList();
        return calculator.compute(series);
    }

    private void requireAllAssetsExist(List<String> symbols) {
        Set<String> found = assetRepository.findAllBySymbolIn(symbols).stream()
                .map(Asset::getSymbol)
                .collect(Collectors.toSet());
        List<String> missing = symbols.stream().filter(symbol -> !found.contains(symbol)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Asset", String.join(", ", missing));
        }
    }
}
