package com.lucadani.backend.optimization;

import java.util.Objects;

/**
 * A rule a portfolio must satisfy.
 * <p>
 * Portfolios are long-only and fully invested: every weight is a fraction of the budget
 * in [0, 1] and the weights sum to 1. Constraints narrow that space further.
 */
public sealed interface PortfolioConstraint {

    /**
     * Bounds applied to every asset.
     */
    record WeightBounds(double min, double max) implements PortfolioConstraint {
        public WeightBounds {
            requireBounds(min, max);
        }
    }

    /**
     * Bounds for a single asset. Combined with {@link WeightBounds} by intersection.
     */
    record AssetWeightBounds(String symbol, double min, double max) implements PortfolioConstraint {
        public AssetWeightBounds {
            Objects.requireNonNull(symbol, "symbol must not be null");
            requireBounds(min, max);
        }
    }

    /**
     * Maximum total weight of the assets in one sector.
     */
    record SectorLimit(String sector, double maxWeight) implements PortfolioConstraint {
        public SectorLimit {
            Objects.requireNonNull(sector, "sector must not be null");
            requireFraction(maxWeight, "maxWeight");
        }
    }

    /**
     * Maximum number of assets with a non-zero weight.
     */
    record MaxAssets(int count) implements PortfolioConstraint {
        public MaxAssets {
            if (count < 1) {
                throw new IllegalArgumentException("count must be at least 1");
            }
        }
    }

    /**
     * Minimum annualized expected return of the portfolio.
     */
    record MinExpectedReturn(double value) implements PortfolioConstraint {
        public MinExpectedReturn {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("value must be a finite number");
            }
        }
    }

    private static void requireBounds(double min, double max) {
        requireFraction(min, "min");
        requireFraction(max, "max");
        if (min > max) {
            throw new IllegalArgumentException("min must not be greater than max");
        }
    }

    private static void requireFraction(double value, String name) {
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalArgumentException(name + " must be between 0 and 1, got " + value);
        }
    }
}
