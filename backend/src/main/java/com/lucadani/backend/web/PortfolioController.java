package com.lucadani.backend.web;

import com.lucadani.backend.service.PortfolioService;
import com.lucadani.backend.service.ReoptimizationService;
import com.lucadani.backend.web.dto.CreatePortfolioRequest;
import com.lucadani.backend.web.dto.PortfolioResponse;
import com.lucadani.backend.web.dto.PortfolioSummaryResponse;
import com.lucadani.backend.web.dto.RenamePortfolioRequest;
import com.lucadani.backend.web.dto.ReoptimizationResponse;
import com.lucadani.backend.web.dto.ScheduleRequest;
import io.swagger.v3.oas.annotations.Operation;
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
    private final ReoptimizationService reoptimizationService;

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

    @Operation(summary = "Set how often the portfolio is reoptimized on its own")
    @PutMapping("/{id}/schedule")
    public PortfolioResponse schedule(@PathVariable Long id, @RequestBody ScheduleRequest request) {
        reoptimizationService.schedule(id, request.frequency());
        return portfolioService.findById(id);
    }

    @Operation(summary = "The latest reoptimizations of the portfolio, newest first")
    @GetMapping("/{id}/reoptimizations")
    public List<ReoptimizationResponse> reoptimizations(@PathVariable Long id) {
        return reoptimizationService.history(id);
    }

    @Operation(summary = "Reoptimize the portfolio now, on the latest prices",
            description = "The outcome is saved either way; a run that cannot satisfy the rules has status FAILED.")
    @PostMapping("/{id}/reoptimizations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReoptimizationResponse reoptimize(@PathVariable Long id) {
        return reoptimizationService.reoptimize(id);
    }

        @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        portfolioService.delete(id);
    }
}
