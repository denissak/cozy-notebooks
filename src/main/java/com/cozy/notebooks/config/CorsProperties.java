package com.cozy.notebooks.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Comma-separated browser origins allowed for cross-origin requests.
 * Bound from {@code cozy.cors.allowed-origins}, typically driven by
 * {@code COZY_CORS_ALLOWED_ORIGINS}.
 */
@ConfigurationProperties(prefix = "cozy.cors")
public class CorsProperties {

    /**
     * Raw comma-separated list from configuration (never "*" — use explicit origins).
     */
    private String allowedOrigins = "";

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? "" : allowedOrigins;
    }

    public List<String> resolvedAllowedOrigins() {
        if (allowedOrigins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
