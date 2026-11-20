package com.lucadani.backend.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lucadani.backend.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Flat view of an asset. Kind-specific fields are omitted when they do not apply. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssetResponse(Long id,
                            AssetType type,
                            String symbol,
                            String name,
                            String sector,
                            String currency,
                            String exchange,
                            BigDecimal expenseRatio,
                            BigDecimal couponRate,
                            LocalDate maturityDate,
                            Instant createdAt,
                            Instant updatedAt) {
    public static AssetResponse from(Asset asset) {
        String exchange = null;
        BigDecimal expenseRatio = null;
        BigDecimal couponRate = null;
        LocalDate maturityDate = null;

        switch (asset) {
            case Stock stock -> exchange = stock.getExchange();
            case Etf etf -> expenseRatio = etf.getExpenseRatio();
            case Bond bond -> {
                couponRate = bond.getCouponRate();
                maturityDate = bond.getMaturityDate();
            }
            default -> throw new IllegalStateException("Unsupported asset kind: " + asset.getClass());
        }

        return new AssetResponse(
                asset.getId(),
                asset.getType(),
                asset.getSymbol(),
                asset.getName(),
                asset.getSector(),
                asset.getCurrency(),
                exchange,
                expenseRatio,
                couponRate,
                maturityDate,
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
