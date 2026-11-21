package com.lucadani.backend.repository;

import com.lucadani.backend.model.Asset;
import com.lucadani.backend.model.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    long countByAsset(Asset asset);
}
