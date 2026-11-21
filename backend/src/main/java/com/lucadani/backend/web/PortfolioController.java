package com.lucadani.backend.web;

import com.lucadani.backend.service.PortfolioService;
import com.lucadani.backend.web.dto.CreatePortfolioRequest;
import com.lucadani.backend.web.dto.PortfolioResponse;
import com.lucadani.backend.web.dto.PortfolioSummaryResponse;
import com.lucadani.backend.web.dto.RenamePortfolioRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Tag(name = "Portfolios", description = "Save, value and manage allocations")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public List<PortfolioSummaryResponse> list() {
        return portfolioService.findAll();
    }

    @GetMapping("/{id}")
    public PortfolioResponse get(@PathVariable Long id) {
        return portfolioService.findById(id);
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> create(@Valid @RequestBody CreatePortfolioRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        PortfolioResponse created = portfolioService.create(request);
        URI location = uriBuilder.path("/api/portfolios/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public PortfolioResponse rename(@PathVariable Long id, @Valid @RequestBody RenamePortfolioRequest request) {
        return portfolioService.rename(id, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        portfolioService.delete(id);
    }
}
