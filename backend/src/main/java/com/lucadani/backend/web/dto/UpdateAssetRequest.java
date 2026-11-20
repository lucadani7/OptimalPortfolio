package com.lucadani.backend.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for updating an asset. The type and symbol cannot change.
 * Kind-specific fields are applied only to the matching asset kind and ignored otherwise.
 */
public record UpdateAssetRequest(@NotBlank @Size(max = 255) String name,
                                 @NotBlank @Size(max = 64) String sector,
                                 @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
                                 @Size(max = 32) String exchange,
                                 @DecimalMin("0") @DecimalMax("1") BigDecimal expenseRatio,
                                 @DecimalMin("0") @DecimalMax("1") BigDecimal couponRate,
                                 LocalDate maturityDate) {
}
