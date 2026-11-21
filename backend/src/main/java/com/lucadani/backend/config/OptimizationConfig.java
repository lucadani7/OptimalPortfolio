package com.lucadani.backend.config;

import com.lucadani.backend.optimization.EqualWeightStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the optimization strategies. Every OptimizationStrategy bean becomes available in the API. */
@Configuration
public class OptimizationConfig {
    @Bean
    public EqualWeightStrategy equalWeightStrategy() {
        return new EqualWeightStrategy();
    }
}
