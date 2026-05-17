package com.cozy.notebooks.security;

import com.cozy.notebooks.exception.UnauthorizedException;
import com.cozy.notebooks.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Parses a Bearer JWT into a {@link CurrentUser} principal when present.
 * Invalid or expired tokens yield HTTP 401 without invoking the controller chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (AuthenticationFilterBypass.shouldBypassAuthenticationFilters(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.length() <= BEARER_PREFIX.length()
                || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            filterChain.doFilter(request, response);
            return;
        }

        String raw = header.substring(BEARER_PREFIX.length()).trim();
        if (raw.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            CurrentUser user = jwtService.parseCurrentUser(raw);
            BearerAuthenticationToken authentication = new BearerAuthenticationToken(user);
            authentication.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UnauthorizedException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static final class BearerAuthenticationToken extends AbstractAuthenticationToken {
        private final CurrentUser principal;

        BearerAuthenticationToken(CurrentUser principal) {
            super(List.of(new SimpleGrantedAuthority("ROLE_USER")));
            this.principal = principal;
        }

        @Override
        public Object getCredentials() {
            return "N/A";
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}
