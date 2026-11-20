package com.lucadani.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("STOCK")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends Asset {
    /** Listing exchange (e.g. "NASDAQ"). */
    @Column(length = 32)
    private String exchange;

    public Stock(String symbol, String name, String sector, String currency, String exchange) {
        super(symbol, name, sector, currency);
        this.exchange = exchange;
    }

    @Override
    public AssetType getType() {
        return AssetType.STOCK;
    }

    public void changeExchange(String exchange) {
        this.exchange = exchange;
    }
}
