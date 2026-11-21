package com.lucadani.backend.repository;

import com.lucadani.backend.model.Portfolio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @EntityGraph(attributePaths = "holdings")
    List<Portfolio> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"holdings", "holdings.asset"})
    Optional<Portfolio> findWithHoldingsById(Long id);
}
