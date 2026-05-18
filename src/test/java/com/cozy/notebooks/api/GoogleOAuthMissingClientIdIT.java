package com.cozy.notebooks.api;

import com.cozy.notebooks.support.AbstractRealAuthIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoogleOAuthMissingClientIdIT extends AbstractRealAuthIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @DynamicPropertySource
    static void googleMissingClientId(DynamicPropertyRegistry registry) {
        registry.add("cozy.auth.google-enabled", () -> "true");
        registry.add("cozy.auth.google-client-id", () -> "");
    }

    @Test
    void googleLogin_returns400_whenGoogleClientIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", "any"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"));
    }
}
