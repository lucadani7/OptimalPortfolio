package com.lucadani.backend.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Batch of closing prices to insert or update for a single asset. */
public record PriceImportRequest(@NotEmpty @Size(max = 10_000) List<@Valid @NotNull PriceEntry> prices) {
}
