package com.lucadani.backend.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lightweight projection of a price point, used for bulk statistical reads. */
public record PriceRow(String symbol, LocalDate tradeDate, BigDecimal close) {
}
