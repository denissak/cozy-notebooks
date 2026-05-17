package com.cozy.notebooks.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * Paths where JWT parsing and mock-user injection must not run so public endpoints
 * stay reachable without a security principal (e.g. {@code /actuator/health} probes).
 */
public final class AuthenticationFilterBypass {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * Matches {@link jakarta.servlet.http.HttpServletRequest#getServletPath()}.
     */
    private static final List<String> PATH_PATTERNS = List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/info/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api/v1/health",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    );

    private AuthenticationFilterBypass() {
    }

    public static boolean shouldBypassAuthenticationFilters(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        for (String pattern : PATH_PATTERNS) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
