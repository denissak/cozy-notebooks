package com.cozy.notebooks.service.auth.google;

/**
 * Verified claims from a Google ID token (signature + issuer + audience already validated).
 */
public record GoogleSignInClaims(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String picture
) {
}
