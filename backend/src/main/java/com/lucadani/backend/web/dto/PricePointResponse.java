package com.lucadani.backend.web.dto;

import com.lucadani.backend.model.PricePoint;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PricePointResponse(LocalDate date, BigDecimal close) {
    public static PricePointResponse from(PricePoint pricePoint) {
        return new PricePointResponse(pricePoint.getTradeDate(), pricePoint.getClose());
    }
}
