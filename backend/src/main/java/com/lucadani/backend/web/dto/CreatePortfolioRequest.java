package com.lucadani.backend.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body for creating a portfolio: a name, and the allocation to compute and save. */
public record CreatePortfolioRequest(
        @NotBlank @Size(max = 255) String name,
        @Valid @NotNull AllocationRequest allocation
) {
}
