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
     * Matches request paths ({@link #requestPath(HttpServletRequest)}) against Ant patterns.
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
            "/api/v1/auth/logout",
            "/api/v1/auth/oauth/google"
    );

    private AuthenticationFilterBypass() {
    }

    public static boolean shouldBypassAuthenticationFilters(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = requestPath(request);
        for (String pattern : PATH_PATTERNS) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prefer {@link HttpServletRequest#getServletPath()}; fall back to {@link HttpServletRequest#getRequestURI()}
     * minus {@link HttpServletRequest#getContextPath()} when the servlet path is empty (e.g. some MockMvc setups).
     */
    private static String requestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isEmpty()) {
            return servletPath;
        }
        String uri = request.getRequestURI();
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri.isEmpty() ? "/" : uri;
    }
}
