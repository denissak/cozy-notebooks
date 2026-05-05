package com.cozy.notebooks.security;

import java.util.UUID;

/**
 * Immutable representation of the authenticated principal.
 * In MVP this is filled by {@link com.cozy.notebooks.security.MockAuthenticationFilter}.
 * When JWT is wired in, the same record will be produced by the JWT filter.
 */
public record CurrentUser(UUID id, String email) {
}
