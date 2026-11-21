package com.lucadani.backend.config;

import com.lucadani.backend.optimization.DiscreteAllocator;
import com.lucadani.backend.optimization.EfficientFrontier;
import com.lucadani.backend.optimization.EqualWeightStrategy;
import com.lucadani.backend.optimization.MaxSharpeStrategy;
import com.lucadani.backend.optimization.MinimumVarianceStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the optimization strategies. Every OptimizationStrategy bean becomes available in the API. */
@Configuration
public class OptimizationConfig {

    @Bean
    public EfficientFrontier efficientFrontier() {
        return new EfficientFrontier();
    }

    @Bean
    public DiscreteAllocator discreteAllocator() {
        return new DiscreteAllocator();
    }

    @Bean
    public EqualWeightStrategy equalWeightStrategy() {
        return new EqualWeightStrategy();
    }

    @Bean
    public MinimumVarianceStrategy minimumVarianceStrategy() {
        return new MinimumVarianceStrategy();
    }

    @Bean
    public MaxSharpeStrategy maxSharpeStrategy(EfficientFrontier efficientFrontier) {
        return new MaxSharpeStrategy(efficientFrontier);
    }
}
