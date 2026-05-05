package com.cozy.notebooks.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * MVP-only filter that injects a fixed user into the SecurityContext.
 * Only active when {@code cozy.security.mock-user-enabled=true}.
 *
 * Replace with a JWT-decoding filter when the auth service ships.
 * The downstream code only depends on {@link CurrentUser} being the principal.
 */
public class MockAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityProperties properties;

    public MockAuthenticationFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (properties.mockUserEnabled() && SecurityContextHolder.getContext().getAuthentication() == null) {
            CurrentUser user = new CurrentUser(properties.mockUserId(), properties.mockUserEmail());
            AbstractAuthenticationToken token = new MockAuthenticationToken(user);
            token.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(token);
        }
        filterChain.doFilter(request, response);
    }

    private static final class MockAuthenticationToken extends AbstractAuthenticationToken {
        private final CurrentUser principal;

        MockAuthenticationToken(CurrentUser principal) {
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
