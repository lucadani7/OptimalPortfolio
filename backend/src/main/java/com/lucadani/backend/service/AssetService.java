package com.lucadani.backend.service;

import com.lucadani.backend.exception.ConflictException;
import com.lucadani.backend.exception.ResourceNotFoundException;
import com.lucadani.backend.model.*;
import com.lucadani.backend.repository.AssetRepository;
import com.lucadani.backend.web.dto.AssetResponse;
import com.lucadani.backend.web.dto.CreateAssetRequest;
import com.lucadani.backend.web.dto.UpdateAssetRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {
    private final AssetRepository assetRepository;

    public List<AssetResponse> findAll(AssetType type) {
        return Optional.ofNullable(type)
                .map(AssetService::entityClassOf)
                .map(assetRepository::findAllOfType)
                .orElseGet(assetRepository::findAllByOrderBySymbolAsc)
                .stream()
                .map(AssetResponse::from)
                .toList();
    }

    public AssetResponse findBySymbol(String symbol) {
        return AssetResponse.from(getAsset(symbol));
    }

    @Transactional
    public AssetResponse create(CreateAssetRequest request) {
        String symbol = normalize(request.symbol());
        if (assetRepository.existsBySymbol(symbol)) {
            throw new ConflictException("Asset '" + symbol + "' already exists");
        }

        Asset asset = switch (request) {
            case CreateAssetRequest.StockRequest r ->
                    new Stock(r.symbol(), r.name(), r.sector(), r.currency(), r.exchange());
            case CreateAssetRequest.EtfRequest r ->
                    new Etf(r.symbol(), r.name(), r.sector(), r.currency(), r.expenseRatio());
            case CreateAssetRequest.BondRequest r ->
                    new Bond(r.symbol(), r.name(), r.sector(), r.currency(), r.couponRate(), r.maturityDate());
        };

        try {
            return AssetResponse.from(assetRepository.saveAndFlush(asset));
        } catch (DataIntegrityViolationException e) {
            // Another request created the same symbol between the existence check and the insert
            throw new ConflictException("Asset '" + symbol + "' already exists");
        }
    }

    @Transactional
    public AssetResponse update(String symbol, UpdateAssetRequest request) {
        Asset asset = getAsset(symbol);
        asset.updateDetails(request.name(), request.sector(), request.currency());

        switch (asset) {
            case Stock stock -> stock.changeExchange(request.exchange());
            case Etf etf -> etf.changeExpenseRatio(request.expenseRatio());
            case Bond bond -> {
                if (request.maturityDate() == null) {
                    throw new IllegalArgumentException("maturityDate is required for bonds");
                }
                bond.updateTerms(request.couponRate(), request.maturityDate());
            }
            default -> throw new IllegalStateException("Unsupported asset kind: " + asset.getClass());
        }

        // Flush so that the version and updatedAt values in the response are current
        assetRepository.flush();
        return AssetResponse.from(asset);
    }

    @Transactional
    public void delete(String symbol) {
        assetRepository.delete(getAsset(symbol));
    }

    private Asset getAsset(String symbol) {
        String normalized = normalize(symbol);
        return assetRepository.findBySymbol(normalized).orElseThrow(() -> new ResourceNotFoundException("Asset", normalized));
    }

    private static String normalize(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static Class<? extends Asset> entityClassOf(AssetType type) {
        return switch (type) {
            case STOCK -> Stock.class;
            case ETF -> Etf.class;
            case BOND -> Bond.class;
        };
    }
}
