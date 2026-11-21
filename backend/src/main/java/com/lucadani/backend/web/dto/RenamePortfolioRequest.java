package com.lucadani.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenamePortfolioRequest(@NotBlank @Size(max = 255) String name) {
}
