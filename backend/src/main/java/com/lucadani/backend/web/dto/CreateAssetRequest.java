package com.lucadani.backend.web.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating an asset. The JSON "type" property selects the concrete kind.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateAssetRequest.StockRequest.class, name = "STOCK"),
        @JsonSubTypes.Type(value = CreateAssetRequest.EtfRequest.class, name = "ETF"),
        @JsonSubTypes.Type(value = CreateAssetRequest.BondRequest.class, name = "BOND")
})
// Tells the OpenAPI generator the real discriminator values ("STOCK", not "StockRequest")
@Schema(discriminatorProperty = "type", discriminatorMapping = {
        @DiscriminatorMapping(value = "STOCK", schema = CreateAssetRequest.StockRequest.class),
        @DiscriminatorMapping(value = "ETF", schema = CreateAssetRequest.EtfRequest.class),
        @DiscriminatorMapping(value = "BOND", schema = CreateAssetRequest.BondRequest.class)
})
public sealed interface CreateAssetRequest {
    String symbol();

    String name();

    String sector();

    String currency();

    record StockRequest(
            @NotBlank @Size(max = 16) String symbol,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 64) String sector,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @Size(max = 32) String exchange
    ) implements CreateAssetRequest {
    }

    record EtfRequest(
            @NotBlank @Size(max = 16) String symbol,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 64) String sector,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @DecimalMin("0") @DecimalMax("1") BigDecimal expenseRatio
    ) implements CreateAssetRequest {
    }

    record BondRequest(
            @NotBlank @Size(max = 16) String symbol,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 64) String sector,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @DecimalMin("0") @DecimalMax("1") BigDecimal couponRate,
            @NotNull LocalDate maturityDate
    ) implements CreateAssetRequest {
    }
}
