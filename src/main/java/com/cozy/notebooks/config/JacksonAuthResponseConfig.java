package com.cozy.notebooks.config;

import com.cozy.notebooks.api.dto.AuthDtos;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Auth user payloads must expose {@code avatarUrl} as {@code null} in JSON despite
 * {@code spring.jackson.default-property-inclusion=non_null}, so clients can rely on the field shape.
 */
@Configuration
public class JacksonAuthResponseConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer authResponseNullWrites() {
        return builder -> builder.postConfigurer(JacksonAuthResponseConfig::enforceAuthDtoInclusionOverrides);
    }

    private static void enforceAuthDtoInclusionOverrides(ObjectMapper objectMapper) {
        JsonInclude.Value always = JsonInclude.Value.construct(
                JsonInclude.Include.ALWAYS,
                JsonInclude.Include.ALWAYS);
        objectMapper.configOverride(AuthDtos.AuthUserResponse.class).setInclude(always);
        objectMapper.configOverride(AuthDtos.AuthTokensResponse.class).setInclude(always);
        objectMapper.configOverride(AuthDtos.MeResponse.class).setInclude(always);
    }
}
