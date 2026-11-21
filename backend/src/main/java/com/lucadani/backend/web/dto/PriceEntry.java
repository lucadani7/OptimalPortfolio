package com.lucadani.backend.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One closing price for one trading day. */
public record PriceEntry(@NotNull LocalDate date,
                         @NotNull @Positive @Digits(integer = 13, fraction = 6) BigDecimal close) {
}
