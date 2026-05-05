package com.cozy.notebooks.security;

import com.cozy.notebooks.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Source of truth for "who is the caller". Read by services so that no
 * controller has to forward the authenticated user manually.
 *
 * The implementation only reads from the {@link SecurityContextHolder}, so
 * swapping the mock filter for a JWT filter requires no changes here.
 */
@Component
public class CurrentUserProvider {

    public CurrentUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CurrentUser principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    public UUID requireId() {
        return require().id();
    }
}
