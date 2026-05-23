package com.cozy.notebooks.security;

import com.cozy.notebooks.logging.MdcKeys;
import jakarta.servlet.FilterChain;
import org.slf4j.MDC;
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
 * When {@code cozy.security.mock-user-enabled=true}, injects a fixed {@link CurrentUser}
 * for business routes only. Skips actuator, OpenAPI, public auth endpoints, and OPTIONS so
 * probes and registration flows never carry an application principal.
 *
 * <p>Downstream code reads {@link CurrentUser} from the {@link SecurityContextHolder}; JWT
 * authentication is handled by {@link JwtAuthenticationFilter} first when a Bearer token is present.
 */
public class MockAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityProperties properties;

    public MockAuthenticationFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    /**
     * Same bypass list as {@link JwtAuthenticationFilter}: never inject mock principal on public routes or OPTIONS.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return AuthenticationFilterBypass.shouldBypassAuthenticationFilters(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (properties.mockUserEnabled()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            CurrentUser user = new CurrentUser(properties.mockUserId(), properties.mockUserEmail());
            AbstractAuthenticationToken token = new MockAuthenticationToken(user);
            token.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(token);
            MDC.put(MdcKeys.USER_ID, user.id().toString());
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
