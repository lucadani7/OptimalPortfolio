package com.lucadani.backend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Enables Spring's cache abstraction. Caffeine is configured in application.yaml. */
@Configuration
@EnableCaching
public class CacheConfig {
}
