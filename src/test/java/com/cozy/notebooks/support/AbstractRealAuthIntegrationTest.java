package com.cozy.notebooks.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integration tests that exercise real JWT authentication (mock-user disabled).
 */
public abstract class AbstractRealAuthIntegrationTest extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void registerRealAuthProperties(DynamicPropertyRegistry registry) {
        registry.add("cozy.security.mock-user-enabled", () -> "false");
        registry.add("cozy.auth.jwt-secret",
                () -> "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        registry.add("cozy.auth.access-token-ttl-minutes", () -> "120");
        registry.add("cozy.auth.refresh-token-ttl-days", () -> "30");
    }
}
