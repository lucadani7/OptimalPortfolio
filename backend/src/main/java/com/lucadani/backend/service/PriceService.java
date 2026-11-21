package com.lucadani.backend.service;

import com.lucadani.backend.model.Asset;
import com.lucadani.backend.repository.PricePointRepository;
import com.lucadani.backend.web.dto.PriceEntry;
import com.lucadani.backend.web.dto.PriceImportResponse;
import com.lucadani.backend.web.dto.PricePointResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceService {
    private static final LocalDate EARLIEST_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate LATEST_DATE = LocalDate.of(9999, 12, 31);
    private static final int BATCH_SIZE = 500;

    private static final String UPSERT_SQL = """
            insert into price_points (asset_id, trade_date, close)
            values (?, ?, ?)
            on conflict (asset_id, trade_date) do update set close = excluded.close
            """;

    private final AssetService assetService;
    private final PricePointRepository pricePointRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<PricePointResponse> findPrices(String symbol, @Nullable LocalDate from, @Nullable LocalDate to) {
        LocalDate start = (from != null) ? from : EARLIEST_DATE;
        LocalDate end = (to != null) ? to : LATEST_DATE;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }

        Asset asset = assetService.getAsset(symbol);
        return pricePointRepository.findByAssetAndTradeDateBetweenOrderByTradeDateAsc(asset, start, end)
                .stream()
                .map(PricePointResponse::from)
                .toList();
    }

    /** Inserts new prices and overwrites existing ones for the same dates. */
    @Transactional
    public PriceImportResponse importPrices(String symbol, List<PriceEntry> prices) {
        Asset asset = assetService.getAsset(symbol);
        Long assetId = asset.getId();

        jdbcTemplate.batchUpdate(UPSERT_SQL, prices, BATCH_SIZE, (statement, entry) -> {
            statement.setLong(1, assetId);
            statement.setObject(2, entry.date());
            statement.setBigDecimal(3, entry.close());
        });

        LocalDate first = prices.stream().map(PriceEntry::date).min(Comparator.naturalOrder()).orElse(null);
        LocalDate last = prices.stream().map(PriceEntry::date).max(Comparator.naturalOrder()).orElse(null);
        return new PriceImportResponse(asset.getSymbol(), prices.size(), first, last);
    }
}
