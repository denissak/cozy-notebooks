package com.cozy.notebooks.config;

import com.cozy.notebooks.security.AuthProperties;
import com.cozy.notebooks.service.auth.google.DefaultGoogleOAuthTokenVerifier;
import com.cozy.notebooks.service.auth.google.GoogleOAuthTokenVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleOAuthConfiguration {

    @Bean
    @ConditionalOnMissingBean(GoogleOAuthTokenVerifier.class)
    public GoogleOAuthTokenVerifier googleOAuthTokenVerifier(AuthProperties authProperties) {
        return new DefaultGoogleOAuthTokenVerifier(authProperties);
    }
}
