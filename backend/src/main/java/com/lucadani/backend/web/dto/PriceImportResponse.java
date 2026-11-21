package com.lucadani.backend.web.dto;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/** Summary of a price import. */
public record PriceImportResponse(String symbol,
                                  int received,
                                  @Nullable LocalDate from,
                                  @Nullable LocalDate to) {
}
