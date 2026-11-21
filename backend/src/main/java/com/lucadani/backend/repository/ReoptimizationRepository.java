package com.lucadani.backend.repository;

import com.lucadani.backend.model.Reoptimization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReoptimizationRepository extends JpaRepository<Reoptimization, Long> {

    /** The 20 most recent runs of a portfolio, newest first. */
    List<Reoptimization> findTop20ByPortfolioIdOrderByCreatedAtDescIdDesc(Long portfolioId);
}
