package com.cozy.notebooks.api;

import com.cozy.notebooks.exception.UnauthorizedException;
import com.cozy.notebooks.repository.UserIdentityRepository;
import com.cozy.notebooks.repository.UserRepository;
import com.cozy.notebooks.service.AuthService;
import com.cozy.notebooks.service.auth.google.GoogleOAuthTokenVerifier;
import com.cozy.notebooks.service.auth.google.GoogleSignInClaims;
import com.cozy.notebooks.support.AbstractRealAuthIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static com.cozy.notebooks.service.AuthService.PROVIDER_GOOGLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoogleOAuthAuthIT extends AbstractRealAuthIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserIdentityRepository userIdentityRepository;

    @MockBean
    GoogleOAuthTokenVerifier googleOAuthTokenVerifier;

    @DynamicPropertySource
    static void googleAuthProps(DynamicPropertyRegistry registry) {
        registry.add("cozy.auth.google-enabled", () -> "true");
        registry.add("cozy.auth.google-client-id", () -> "unit-test.apps.googleusercontent.com");
    }

    @BeforeEach
    void resetGoogleVerifierMock() {
        Mockito.reset(googleOAuthTokenVerifier);
    }

    @Test
    void googleLogin_createsUser_andGoogleIdentity() throws Exception {
        String sub = "google-sub-" + UUID.randomUUID();
        String email = "google-new-" + UUID.randomUUID() + "@example.com";
        when(googleOAuthTokenVerifier.verify(anyString())).thenReturn(
                new GoogleSignInClaims(sub, email, true, "Goog User", null));

        long usersBefore = userRepository.count();
        long identitiesBefore = userIdentityRepository.count();

        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "mock-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value(email.toLowerCase()));

        assertThat(userRepository.count()).isEqualTo(usersBefore + 1);
        assertThat(userIdentityRepository.count()).isEqualTo(identitiesBefore + 1);
        assertThat(userIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull(PROVIDER_GOOGLE, sub))
                .isPresent();
    }

    @Test
    void googleLogin_sameSubject_doesNotCreateDuplicateUser() throws Exception {
        String sub = "google-sub-stable-" + UUID.randomUUID();
        String email = "google-stable-" + UUID.randomUUID() + "@example.com";
        when(googleOAuthTokenVerifier.verify(anyString())).thenReturn(
                new GoogleSignInClaims(sub, email, true, "Name", null));

        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "first"))))
                .andExpect(status().isOk());

        long afterFirst = userRepository.count();

        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "second"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email.toLowerCase()));

        assertThat(userRepository.count()).isEqualTo(afterFirst);
    }

    @Test
    void googleLogin_linksToExistingEmailPasswordUser_whenGoogleEmailVerified() throws Exception {
        String email = "link-" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";
        MvcResult reg = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        UUID userId = UUID.fromString(objectMapper.readTree(reg.getResponse().getContentAsString())
                .get("user").get("id").asText());

        String sub = "google-link-sub-" + UUID.randomUUID();
        when(googleOAuthTokenVerifier.verify(anyString())).thenReturn(
                new GoogleSignInClaims(sub, email, true, "Linked", null));

        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "mock-google"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.user.email").value(email.toLowerCase()));

        assertThat(userIdentityRepository.findByProviderAndProviderSubjectAndDeletedAtIsNull(PROVIDER_GOOGLE, sub))
                .isPresent();
    }

    @Test
    void googleLogin_doesNotLink_whenGoogleEmailNotVerified_andEmailPasswordExists() throws Exception {
        String email = "noverify-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isOk());

        String sub = "google-unverified-" + UUID.randomUUID();
        when(googleOAuthTokenVerifier.verify(anyString())).thenReturn(
                new GoogleSignInClaims(sub, email, false, "X", null));

        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "mock-google"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"));
    }

    @Test
    void googleLogin_invalidToken_returns401() throws Exception {
        when(googleOAuthTokenVerifier.verify(anyString()))
                .thenThrow(new UnauthorizedException("Invalid Google ID token"));

        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "bad"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void notebookEndpoint_works_withAccessTokenFromGoogleLogin() throws Exception {
        String sub = "google-jwt-" + UUID.randomUUID();
        String email = "google-jwt-" + UUID.randomUUID() + "@example.com";
        when(googleOAuthTokenVerifier.verify(anyString())).thenReturn(
                new GoogleSignInClaims(sub, email, true, "JWT User", null));

        MvcResult res = mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "mock-google"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsString());
        String access = body.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/notebooks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk());
    }
}
