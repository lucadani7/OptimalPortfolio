package com.lucadani.backend.web.dto;

import com.lucadani.backend.optimization.EfficientFrontier;
import com.lucadani.backend.optimization.PortfolioConstraint;
import com.lucadani.backend.service.StatisticsQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for tracing the efficient frontier.
 * The period defaults to the last year, the risk-free rate to 0 and the number of points to 25.
 */
public record FrontierRequest(
        @NotEmpty @Size(max = StatisticsQuery.MAX_ASSETS) List<@NotBlank String> symbols,
        @Nullable LocalDate from,
        @Nullable LocalDate to,
        @Nullable List<@Valid @NotNull ConstraintRequest> constraints,
        @Nullable @DecimalMin("-1") @DecimalMax("1") Double riskFreeRate,
        @Nullable @Min(EfficientFrontier.MIN_POINTS) @Max(EfficientFrontier.MAX_POINTS) Integer points
) {

    public static final int DEFAULT_POINTS = 25;

    public List<PortfolioConstraint> domainConstraints() {
        return (constraints == null) ? List.of() : constraints.stream().map(ConstraintRequest::toDomain).toList();
    }

    public double riskFreeRateOrZero() {
        return (riskFreeRate == null) ? 0 : riskFreeRate;
    }

    public int pointsOrDefault() {
        return (points == null) ? DEFAULT_POINTS : points;
    }
}
