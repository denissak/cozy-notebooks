package com.cozy.notebooks.service;

import com.cozy.notebooks.api.dto.AuthDtos.AuthTokensResponse;
import com.cozy.notebooks.api.dto.AuthDtos.AuthUserResponse;
import com.cozy.notebooks.api.dto.AuthDtos.GoogleLoginRequest;
import com.cozy.notebooks.api.dto.AuthDtos.LoginRequest;
import com.cozy.notebooks.api.dto.AuthDtos.LogoutRequest;
import com.cozy.notebooks.api.dto.AuthDtos.MeResponse;
import com.cozy.notebooks.api.dto.AuthDtos.RefreshRequest;
import com.cozy.notebooks.api.dto.AuthDtos.RefreshTokensResponse;
import com.cozy.notebooks.api.dto.AuthDtos.RegisterRequest;
import com.cozy.notebooks.domain.RefreshTokenSessionEntity;
import com.cozy.notebooks.domain.UserEntity;
import com.cozy.notebooks.domain.UserIdentityEntity;
import com.cozy.notebooks.exception.BadRequestException;
import com.cozy.notebooks.exception.ConflictException;
import com.cozy.notebooks.exception.UnauthorizedException;
import com.cozy.notebooks.repository.RefreshTokenSessionRepository;
import com.cozy.notebooks.repository.UserIdentityRepository;
import com.cozy.notebooks.repository.UserRepository;
import com.cozy.notebooks.security.AuthProperties;
import com.cozy.notebooks.security.CurrentUser;
import com.cozy.notebooks.security.CurrentUserProvider;
import com.cozy.notebooks.service.auth.google.GoogleOAuthTokenVerifier;
import com.cozy.notebooks.service.auth.google.GoogleSignInClaims;
import com.cozy.notebooks.service.crypto.Sha256Hex;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    public static final String PROVIDER_EMAIL = "email";
    public static final String PROVIDER_GOOGLE = "google";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties authProperties;
    private final Sha256Hex sha256Hex;
    private final CurrentUserProvider currentUserProvider;
    private final GoogleOAuthTokenVerifier googleOAuthTokenVerifier;

    public AuthService(UserRepository userRepository,
                       UserIdentityRepository userIdentityRepository,
                       RefreshTokenSessionRepository refreshTokenSessionRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthProperties authProperties,
                       Sha256Hex sha256Hex,
                       CurrentUserProvider currentUserProvider,
                       GoogleOAuthTokenVerifier googleOAuthTokenVerifier) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authProperties = authProperties;
        this.sha256Hex = sha256Hex;
        this.currentUserProvider = currentUserProvider;
        this.googleOAuthTokenVerifier = googleOAuthTokenVerifier;
    }

    @Transactional
    public AuthTokensResponse registerWithEmail(RegisterRequest request, ClientMeta meta) {
        String normalizedEmail = normalizeEmail(request.email());
        ensureEmailAvailable(normalizedEmail);

        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .email(normalizedEmail)
                .displayName(null)
                .build();
        userRepository.save(user);

        UserIdentityEntity identity = UserIdentityEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider(PROVIDER_EMAIL)
                .providerSubject(normalizedEmail)
                .email(normalizedEmail)
                .emailVerified(false)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        userIdentityRepository.save(identity);

        return issueTokens(user, normalizedEmail, meta);
    }

    @Transactional
    public AuthTokensResponse loginWithEmail(LoginRequest request, ClientMeta meta) {
        String normalizedEmail = normalizeEmail(request.email());
        UserIdentityEntity identity = userIdentityRepository
                .findByProviderAndProviderSubjectAndDeletedAtIsNull(PROVIDER_EMAIL, normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (identity.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), identity.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        identity.setLastLoginAt(OffsetDateTime.now());
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(identity.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        return issueTokens(user, normalizedEmail, meta);
    }

    /**
     * Google Sign-In using a verified ID token. Linking rules:
     * <ul>
     *   <li>{@code provider_subject} for Google is always the Google {@code sub}, never email.</li>
     *   <li>If {@code email_verified=true}, link to an existing email/password identity with the same
     *       normalized email (same {@code users.id}), or to an orphan {@code users} row with that email.</li>
     *   <li>If {@code email_verified=false}, never auto-link to an existing email/password account (unsafe).
     *       If that email is already taken, return {@link ConflictException}. Otherwise create a new user
     *       with only the Google identity.</li>
     * </ul>
     */
    @Transactional
    public AuthTokensResponse loginWithGoogle(GoogleLoginRequest request, ClientMeta meta) {
        validateGoogleAuthConfigured();
        GoogleSignInClaims claims = googleOAuthTokenVerifier.verify(request.idToken());

        if (claims.email() == null || claims.email().isBlank()) {
            throw new BadRequestException("Google account email is required");
        }

        String normalizedEmail = normalizeEmail(claims.email());

        Optional<UserIdentityEntity> existingGoogle = userIdentityRepository
                .findByProviderAndProviderSubjectAndDeletedAtIsNull(PROVIDER_GOOGLE, claims.subject());
        if (existingGoogle.isPresent()) {
            UserIdentityEntity googleIdentity = existingGoogle.get();
            googleIdentity.setLastLoginAt(OffsetDateTime.now());
            googleIdentity.setEmail(normalizedEmail);
            googleIdentity.setEmailVerified(claims.emailVerified());
            userIdentityRepository.save(googleIdentity);

            UserEntity user = userRepository.findByIdAndDeletedAtIsNull(googleIdentity.getUserId())
                    .orElseThrow(() -> new UnauthorizedException("Invalid Google ID token"));
            return issueTokens(user, normalizeEmail(user.getEmail()), meta);
        }

        if (claims.emailVerified()) {
            Optional<UserIdentityEntity> emailIdentity = userIdentityRepository
                    .findByProviderAndProviderSubjectAndDeletedAtIsNull(PROVIDER_EMAIL, normalizedEmail);
            if (emailIdentity.isPresent()) {
                return createLinkedGoogleIdentity(emailIdentity.get().getUserId(), claims, normalizedEmail, meta);
            }
            Optional<UserEntity> userByEmail = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail);
            if (userByEmail.isPresent()) {
                return createLinkedGoogleIdentity(userByEmail.get().getId(), claims, normalizedEmail, meta);
            }
            return createNewGoogleUser(claims, normalizedEmail, meta);
        }

        if (userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail).isPresent()
                || userIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull(PROVIDER_EMAIL,
                        normalizedEmail).isPresent()) {
            throw new ConflictException(
                    "An account with this email already exists. Verify your Google email or sign in with email and password.");
        }

        return createNewGoogleUser(claims, normalizedEmail, meta);
    }

    private void validateGoogleAuthConfigured() {
        if (!authProperties.googleEnabled()) {
            throw new BadRequestException("Google sign-in is disabled");
        }
        String clientId = authProperties.googleClientId();
        if (clientId == null || clientId.isBlank()) {
            throw new BadRequestException("Google sign-in is not configured");
        }
    }

    private AuthTokensResponse createLinkedGoogleIdentity(UUID userId,
                                                          GoogleSignInClaims claims,
                                                          String normalizedEmail,
                                                          ClientMeta meta) {
        UserIdentityEntity googleIdentity = UserIdentityEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider(PROVIDER_GOOGLE)
                .providerSubject(claims.subject())
                .email(normalizedEmail)
                .emailVerified(claims.emailVerified())
                .passwordHash(null)
                .build();
        googleIdentity.setLastLoginAt(OffsetDateTime.now());
        userIdentityRepository.save(googleIdentity);

        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid Google ID token"));
        return issueTokens(user, normalizeEmail(user.getEmail()), meta);
    }

    private AuthTokensResponse createNewGoogleUser(GoogleSignInClaims claims, String normalizedEmail, ClientMeta meta) {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId)
                .email(normalizedEmail)
                .displayName(claims.name())
                .build();
        userRepository.save(user);

        UserIdentityEntity googleIdentity = UserIdentityEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .provider(PROVIDER_GOOGLE)
                .providerSubject(claims.subject())
                .email(normalizedEmail)
                .emailVerified(claims.emailVerified())
                .passwordHash(null)
                .build();
        googleIdentity.setLastLoginAt(OffsetDateTime.now());
        userIdentityRepository.save(googleIdentity);

        return issueTokens(user, normalizedEmail, meta);
    }

    @Transactional
    public RefreshTokensResponse refreshToken(RefreshRequest request, ClientMeta meta) {
        RefreshTokenSessionEntity session = loadActiveSession(request.refreshToken());

        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        session.setRevokedAt(OffsetDateTime.now());
        refreshTokenSessionRepository.save(session);

        String normalizedEmail = user.getEmail();
        RawRefreshToken rotated = newRawRefreshToken();
        persistSession(user.getId(), rotated, meta);

        String access = jwtService.createAccessToken(user.getId(), normalizedEmail);
        return new RefreshTokensResponse(access, rotated.raw());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        RefreshTokenSessionEntity session = refreshTokenSessionRepository
                .findByRefreshTokenHash(sha256Hex.hashUtf8(request.refreshToken()))
                .orElse(null);
        if (session == null || session.isRevoked()) {
            return;
        }
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return;
        }
        session.setRevokedAt(OffsetDateTime.now());
        refreshTokenSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUserMe() {
        CurrentUser user = currentUserProvider.require();
        return new MeResponse(user.id(), user.email());
    }

    private AuthTokensResponse issueTokens(UserEntity user, String normalizedEmail, ClientMeta meta) {
        RawRefreshToken refresh = newRawRefreshToken();
        persistSession(user.getId(), refresh, meta);
        String access = jwtService.createAccessToken(user.getId(), normalizedEmail);
        return new AuthTokensResponse(
                access,
                refresh.raw(),
                new AuthUserResponse(user.getId(), normalizedEmail)
        );
    }

    private void persistSession(UUID userId, RawRefreshToken token, ClientMeta meta) {
        OffsetDateTime now = OffsetDateTime.now();
        RefreshTokenSessionEntity session = RefreshTokenSessionEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deviceId(null)
                .refreshTokenHash(token.hash())
                .expiresAt(now.plusDays(authProperties.refreshTokenTtlDays()))
                .revokedAt(null)
                .createdAt(now)
                .lastUsedAt(now)
                .userAgent(meta.userAgent())
                .ipAddress(meta.ipAddress())
                .build();
        refreshTokenSessionRepository.save(session);
    }

    private RefreshTokenSessionEntity loadActiveSession(String rawRefreshToken) {
        String hash = sha256Hex.hashUtf8(rawRefreshToken);
        RefreshTokenSessionEntity session = refreshTokenSessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        OffsetDateTime now = OffsetDateTime.now();
        if (session.isRevoked() || session.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        session.setLastUsedAt(now);
        refreshTokenSessionRepository.save(session);
        return session;
    }

    private void ensureEmailAvailable(String normalizedEmail) {
        if (userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail).isPresent()) {
            throw new ConflictException("Email already registered");
        }
        if (userIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull(PROVIDER_EMAIL,
                normalizedEmail).isPresent()) {
            throw new ConflictException("Email already registered");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private RawRefreshToken newRawRefreshToken() {
        byte[] buf = new byte[32];
        SECURE_RANDOM.nextBytes(buf);
        String raw = HEX.formatHex(buf);
        return new RawRefreshToken(raw, sha256Hex.hashUtf8(raw));
    }

    private record RawRefreshToken(String raw, String hash) {
    }

    public record ClientMeta(String userAgent, String ipAddress) {
    }
}
