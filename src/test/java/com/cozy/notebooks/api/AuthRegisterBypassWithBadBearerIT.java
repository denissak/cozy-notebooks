package com.cozy.notebooks.api;

import com.cozy.notebooks.support.AbstractRealAuthIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthRegisterBypassWithBadBearerIT extends AbstractRealAuthIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void register_works_whenAuthorizationContainsInvalidBearer() throws Exception {
        String email = "bypass-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"
                        ))))
                .andExpect(status().isOk());
    }
}
