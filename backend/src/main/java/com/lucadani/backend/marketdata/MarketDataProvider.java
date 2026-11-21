package com.lucadani.backend.marketdata;

import com.lucadani.backend.model.Asset;
import com.lucadani.backend.web.dto.PriceEntry;

import java.time.LocalDate;
import java.util.List;

/** A source of daily closing prices, e.g. a simulator or a market data API. */
public interface MarketDataProvider {

    /** Short identifier shown in logs, e.g. "simulated". */
    String name();

    /**
     * Daily closes of {@code asset} for the trading days from {@code from} to {@code to}, both included,
     * ordered by date. Days without a price are left out.
     */
    List<PriceEntry> fetchPrices(Asset asset, LocalDate from, LocalDate to);
}
