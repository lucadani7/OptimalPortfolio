package com.lucadani.backend.config;

import com.lucadani.backend.analytics.StatisticsCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes the framework-free analytics classes as Spring beans. */
@Configuration
public class AnalyticsConfig {

    @Bean
    public StatisticsCalculator statisticsCalculator() {
        return new StatisticsCalculator();
    }
}
