package com.lucadani.backend.repository;

import com.lucadani.backend.model.Asset;
import com.lucadani.backend.model.PricePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PricePointRepository extends JpaRepository<PricePoint, Long> {
    List<PricePoint> findByAssetAndTradeDateBetweenOrderByTradeDateAsc(Asset asset, LocalDate from, LocalDate to);

    boolean existsByAsset(Asset asset);
}
