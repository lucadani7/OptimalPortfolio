package com.lucadani.backend.web;

import com.lucadani.backend.service.StatisticsQuery;
import com.lucadani.backend.service.StatisticsService;
import com.lucadani.backend.web.dto.StatisticsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Annualized returns, volatilities, covariances and correlations")
public class StatisticsController {
    private final StatisticsService statisticsService;

    @GetMapping
    public StatisticsResponse get(
            @RequestParam List<String> symbols,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to) {
        StatisticsQuery query = StatisticsQuery.of(symbols, from, to);
        return StatisticsResponse.of(statisticsService.getStatistics(query));
    }
}
