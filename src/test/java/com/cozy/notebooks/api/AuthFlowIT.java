package com.cozy.notebooks.api;

import com.cozy.notebooks.support.AbstractRealAuthIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIT extends AbstractRealAuthIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void register_emailUser_success() throws Exception {
        String email = randomEmail();
        Tokens tokens = register(email, "Password123!");

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.userId()).isNotNull();
        assertThat(tokens.userEmail()).isEqualToIgnoringCase(email);
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = randomEmail();
        register(email, "Password123!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "DifferentPass9!"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"));
    }

    @Test
    void login_correctPassword_returnsTokens() throws Exception {
        String email = randomEmail();
        String password = "Password123!";
        register(email, password);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value(email.toLowerCase()));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String email = randomEmail();
        register(email, "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "WrongPass999!"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void refresh_returnsNewAccessToken() throws Exception {
        String email = randomEmail();
        Tokens initial = register(email, "Password123!");

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", initial.refreshToken()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(refreshed.getResponse().getContentAsString());
        assertThat(body.get("accessToken").asText()).isNotEqualTo(initial.accessToken());
        assertThat(body.get("refreshToken").asText()).isNotEqualTo(initial.refreshToken());
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        Tokens tokens = register(randomEmail(), "Password123!");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", tokens.refreshToken()
                        ))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", tokens.refreshToken()
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }

    @Test
    void getMe_withBearerJwt_returnsCurrentUser() throws Exception {
        Tokens tokens = register(randomEmail(), "Password123!");

        JsonNode me = parseJson(mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tokens.userId().toString()))
                .andExpect(jsonPath("$.email").value(tokens.userEmail()))
                .andReturn());
        assertExplicitJsonNullField(me, "avatarUrl");
    }

    @Test
    void register_login_me_responsesExposeAvatarUrl() throws Exception {
        String email = randomEmail();
        String password = "Password123!";

        JsonNode registration = parseJson(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(registration.has("accessToken")).isTrue();
        assertExplicitJsonNullField(registration.path("user"), "avatarUrl");

        JsonNode loggedIn = parseJson(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(loggedIn.has("accessToken")).isTrue();
        assertExplicitJsonNullField(loggedIn.path("user"), "avatarUrl");

        JsonNode me = parseJson(mockMvc.perform(get("/api/v1/auth/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loggedIn.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(me.get("email").asText()).isEqualToIgnoringCase(email);
        assertExplicitJsonNullField(me, "avatarUrl");
    }

    @Test
    void businessEndpoint_withoutJwt_returns401_whenMockDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/notebooks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void businessEndpoint_withValidJwt_returns200_whenMockDisabled() throws Exception {
        Tokens tokens = register(randomEmail(), "Password123!");

        mockMvc.perform(get("/api/v1/notebooks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    private JsonNode parseJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Spring's JsonPath {@code exists()} treats explicit JSON {@code null} as "no value" — verify the key is present instead. */
    private static void assertExplicitJsonNullField(JsonNode container, String field) {
        assertThat(container.has(field)).as("JSON must expose key \"%s\"", field).isTrue();
        assertThat(container.get(field).isNull()).as("Expected \"%s\" to be JSON null", field).isTrue();
    }

    private Tokens register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Tokens(
                json.get("accessToken").asText(),
                json.get("refreshToken").asText(),
                UUID.fromString(json.get("user").get("id").asText()),
                json.get("user").get("email").asText()
        );
    }

    private static String randomEmail() {
        return "auth-" + UUID.randomUUID() + "@example.com";
    }

    private record Tokens(String accessToken, String refreshToken, UUID userId, String userEmail) {
    }
}
