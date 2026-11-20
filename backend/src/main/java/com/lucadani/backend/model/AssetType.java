package com.lucadani.backend.model;

// Kind of asset. The enum names are also the discriminator values stored in {@code assets.asset_type}.
public enum AssetType {
    STOCK,
    ETF,
    BOND
}
