package com.lucadani.backend.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "price_points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PricePoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Asset asset;

    @Column(nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal close;

    public PricePoint(Asset asset, LocalDate tradeDate, BigDecimal close) {
        this.asset = Objects.requireNonNull(asset, "asset must not be null");
        this.tradeDate = Objects.requireNonNull(tradeDate, "tradeDate must not be null");
        if (close == null || close.signum() <= 0) {
            throw new IllegalArgumentException("close must be positive");
        }
        this.close = close;
    }
}
