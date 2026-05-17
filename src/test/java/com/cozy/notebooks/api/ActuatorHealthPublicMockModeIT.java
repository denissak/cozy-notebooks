package com.cozy.notebooks.api;

import com.cozy.notebooks.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActuatorHealthPublicMockModeIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void actuatorHealth_returns200_withoutAuthorization() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealth_returns200_withInvalidBearer_soProbesAreNotBrokenByGarbageAuthHeader() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt"))
                .andExpect(status().isOk());
    }
}
