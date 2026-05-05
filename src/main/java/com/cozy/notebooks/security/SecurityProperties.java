package com.cozy.notebooks.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties(prefix = "cozy.security")
public record SecurityProperties(
        boolean mockUserEnabled,
        UUID mockUserId,
        String mockUserEmail
) {
}
