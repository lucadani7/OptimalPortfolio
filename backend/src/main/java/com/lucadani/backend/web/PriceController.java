package com.lucadani.backend.web;

import com.lucadani.backend.service.PriceService;
import com.lucadani.backend.web.dto.PriceImportRequest;
import com.lucadani.backend.web.dto.PriceImportResponse;
import com.lucadani.backend.web.dto.PricePointResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/assets/{symbol}/prices")
@RequiredArgsConstructor
@Tag(name = "Prices", description = "Historical closing prices of an asset")
public class PriceController {
    private final PriceService priceService;

    @GetMapping
    public List<PricePointResponse> list(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to) {
        return priceService.findPrices(symbol, from, to);
    }

    @PostMapping
    public PriceImportResponse importPrices(@PathVariable String symbol,
                                            @Valid @RequestBody PriceImportRequest request) {
        return priceService.importPrices(symbol, request.prices());
    }
}
