package com.lucadani.backend.web.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.lucadani.backend.optimization.PortfolioConstraint;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.Locale;

/** A constraint as sent by the client. The JSON "type" property selects the kind. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConstraintRequest.WeightBoundsRequest.class, name = "WEIGHT_BOUNDS"),
        @JsonSubTypes.Type(value = ConstraintRequest.AssetWeightBoundsRequest.class, name = "ASSET_WEIGHT_BOUNDS"),
        @JsonSubTypes.Type(value = ConstraintRequest.SectorLimitRequest.class, name = "SECTOR_LIMIT"),
        @JsonSubTypes.Type(value = ConstraintRequest.MaxAssetsRequest.class, name = "MAX_ASSETS"),
        @JsonSubTypes.Type(value = ConstraintRequest.MinExpectedReturnRequest.class, name = "MIN_EXPECTED_RETURN")
})
// Tells the OpenAPI generator the real discriminator values ("WEIGHT_BOUNDS", not "WeightBoundsRequest")
@Schema(discriminatorProperty = "type", discriminatorMapping = {
        @DiscriminatorMapping(value = "WEIGHT_BOUNDS", schema = ConstraintRequest.WeightBoundsRequest.class),
        @DiscriminatorMapping(value = "ASSET_WEIGHT_BOUNDS", schema = ConstraintRequest.AssetWeightBoundsRequest.class),
        @DiscriminatorMapping(value = "SECTOR_LIMIT", schema = ConstraintRequest.SectorLimitRequest.class),
        @DiscriminatorMapping(value = "MAX_ASSETS", schema = ConstraintRequest.MaxAssetsRequest.class),
        @DiscriminatorMapping(value = "MIN_EXPECTED_RETURN", schema = ConstraintRequest.MinExpectedReturnRequest.class)
})
public sealed interface ConstraintRequest {
    PortfolioConstraint toDomain();

    record WeightBoundsRequest(
            @NotNull @DecimalMin("0") @DecimalMax("1") Double min,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double max
    ) implements ConstraintRequest {
        @Override
        public PortfolioConstraint toDomain() {
            return new PortfolioConstraint.WeightBounds(min, max);
        }
    }

    record AssetWeightBoundsRequest(
            @NotBlank String symbol,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double min,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double max
    ) implements ConstraintRequest {
        @Override
        public PortfolioConstraint toDomain() {
            return new PortfolioConstraint.AssetWeightBounds(symbol.trim().toUpperCase(Locale.ROOT), min, max);
        }
    }

    record SectorLimitRequest(
            @NotBlank String sector,
            @NotNull @DecimalMin("0") @DecimalMax("1") Double maxWeight
    ) implements ConstraintRequest {
        @Override
        public PortfolioConstraint toDomain() {
            return new PortfolioConstraint.SectorLimit(sector.trim(), maxWeight);
        }
    }

    record MaxAssetsRequest(@NotNull @Min(1) Integer count) implements ConstraintRequest {
        @Override
        public PortfolioConstraint toDomain() {
            return new PortfolioConstraint.MaxAssets(count);
        }
    }

    record MinExpectedReturnRequest(@NotNull Double value) implements ConstraintRequest {
        @Override
        public PortfolioConstraint toDomain() {
            return new PortfolioConstraint.MinExpectedReturn(value);
        }
    }
}
