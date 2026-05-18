package com.cozy.notebooks.service.auth.google;

import com.cozy.notebooks.exception.UnauthorizedException;
import com.cozy.notebooks.security.AuthProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class DefaultGoogleOAuthTokenVerifier implements GoogleOAuthTokenVerifier {

    private final AuthProperties authProperties;

    private final Object verifierLock = new Object();
    private volatile GoogleIdTokenVerifier cachedVerifier;
    private volatile String cachedAudienceClientId;

    public DefaultGoogleOAuthTokenVerifier(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public GoogleSignInClaims verify(String rawIdToken) {
        String audience = authProperties.googleClientId() == null ? "" : authProperties.googleClientId().trim();
        if (audience.isEmpty()) {
            throw new UnauthorizedException("Invalid Google ID token");
        }
        try {
            GoogleIdTokenVerifier verifier = verifier(audience);
            GoogleIdToken idToken = verifier.verify(rawIdToken);
            if (idToken == null) {
                throw new UnauthorizedException("Invalid Google ID token");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            boolean verified = Boolean.TRUE.equals(payload.getEmailVerified());
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            return new GoogleSignInClaims(payload.getSubject(), email, verified, name, picture);
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid Google ID token");
        }
    }

    private GoogleIdTokenVerifier verifier(String audience) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier snapshot = cachedVerifier;
        if (snapshot != null && audience.equals(cachedAudienceClientId)) {
            return snapshot;
        }
        synchronized (verifierLock) {
            snapshot = cachedVerifier;
            if (snapshot != null && audience.equals(cachedAudienceClientId)) {
                return snapshot;
            }
            GoogleIdTokenVerifier built = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(audience))
                    .build();
            cachedVerifier = built;
            cachedAudienceClientId = audience;
            return built;
        }
    }
}
