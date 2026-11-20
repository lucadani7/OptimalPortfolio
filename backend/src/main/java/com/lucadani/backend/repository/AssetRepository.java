package com.lucadani.backend.repository;

import com.lucadani.backend.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);

    List<Asset> findAllByOrderBySymbolAsc();

    /** Polymorphic query: returns only the assets of the given concrete class. */
    @Query("select a from Asset a where type(a) = :type order by a.symbol")
    List<Asset> findAllOfType(@Param("type") Class<? extends Asset> type);
}
