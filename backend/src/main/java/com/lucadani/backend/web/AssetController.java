package com.lucadani.backend.web;

import com.lucadani.backend.model.AssetType;
import com.lucadani.backend.service.AssetService;
import com.lucadani.backend.web.dto.AssetResponse;
import com.lucadani.backend.web.dto.CreateAssetRequest;
import com.lucadani.backend.web.dto.UpdateAssetRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Manage tradable assets")
public class AssetController {
    private final AssetService assetService;

    @GetMapping
    public List<AssetResponse> list(@RequestParam @Nullable AssetType type) {
        return assetService.findAll(type);
    }

    @GetMapping("/{symbol}")
    public AssetResponse get(@PathVariable String symbol) {
        return assetService.findBySymbol(symbol);
    }

    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody CreateAssetRequest request,
                                                UriComponentsBuilder uriBuilder) {
        AssetResponse created = assetService.create(request);
        URI location = uriBuilder.path("/api/assets/{symbol}").buildAndExpand(created.symbol()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{symbol}")
    public AssetResponse update(@PathVariable String symbol, @Valid @RequestBody UpdateAssetRequest request) {
        return assetService.update(symbol, request);
    }

    @DeleteMapping("/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String symbol) {
        assetService.delete(symbol);
    }
}
