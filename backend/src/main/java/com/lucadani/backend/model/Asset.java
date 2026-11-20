package com.lucadani.backend.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "assets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "asset_type", discriminatorType = DiscriminatorType.STRING, length = 16)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ticker symbol, always stored in upper case (e.g. "AAPL"). Immutable once created. */
    @Column(nullable = false, unique = true, updatable = false, length = 16)
    private String symbol;

    @Column(nullable = false)
    private String name;

    /** Economic sector, used by sector-limit constraints (e.g. "Technology"). */
    @Column(nullable = false, length = 64)
    private String sector;

    /** ISO 4217 currency code (e.g. "USD"). */
    @Column(nullable = false, length = 3)
    private String currency;

    @Version
    private long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Asset(String symbol, String name, String sector, String currency) {
        this.symbol = normalizeCode(symbol, "symbol");
        updateDetails(name, sector, currency);
    }

    /** The concrete kind of this asset. */
    public abstract AssetType getType();

    public void updateDetails(String name, String sector, String currency) {
        this.name = requireText(name, "name");
        this.sector = requireText(sector, "sector");
        this.currency = normalizeCode(currency, "currency");
    }

    private static String normalizeCode(String value, String field) {
        return requireText(value, field).toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }

    /** Assets are identified by their symbol, which is unique and never changes. */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset other)) return false;
        return symbol != null && symbol.equals(other.getSymbol());
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(symbol);
    }

    @Override
    public String toString() {
        return getType() + "[" + symbol + "]";
    }
}
