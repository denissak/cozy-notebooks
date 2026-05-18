package com.cozy.notebooks.service.auth.google;

/**
 * Verifies Google ID tokens using Google's libraries (no hand-rolled JWKS logic).
 */
public interface GoogleOAuthTokenVerifier {

    GoogleSignInClaims verify(String rawIdToken);
}
