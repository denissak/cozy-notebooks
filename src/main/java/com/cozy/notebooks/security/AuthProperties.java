package com.cozy.notebooks.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cozy.auth")
public record AuthProperties(
        String jwtSecret,
        int accessTokenTtlMinutes,
        int refreshTokenTtlDays,
        boolean googleEnabled,
        String googleClientId
) {
}
