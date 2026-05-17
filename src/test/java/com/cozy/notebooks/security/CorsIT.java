package com.cozy.notebooks.security;

import com.cozy.notebooks.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS is driven by {@code cozy.cors.allowed-origins}. Tests use
 * {@code application-test.yml}, which lists Vercel + localhost origins.
 */
class CorsIT extends AbstractIntegrationTest {

    private static final String ALLOWED = "http://localhost:5173";
    private static final String VERCEL_ORIGIN = "https://cozy-notebooks.vercel.app";
    private static final String DISALLOWED = "https://malicious.example";

    @Autowired
    MockMvc mockMvc;

    @Test
    void optionsPreflight_forApi_includesAllowOrigin_forConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/notebooks")
                        .header(HttpHeaders.ORIGIN, ALLOWED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("OPTIONS")));
        // With allowCredentials=false, Spring typically omits Access-Control-Allow-Credentials
        // rather than sending "false" — browsers treat missing as non-credentialed.
    }

    @Test
    void optionsPreflight_forActuator_includesAllowOrigin() throws Exception {
        mockMvc.perform(options("/actuator/health")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    @Test
    void get_withAllowedOrigin_reflectsAccessControlAllowOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header(HttpHeaders.ORIGIN, ALLOWED))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED));
    }

    @Test
    void get_actuatorHealth_withVercelOrigin_returnsAccessControlAllowOrigin() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header(HttpHeaders.ORIGIN, VERCEL_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VERCEL_ORIGIN));
    }

    @Test
    void preflight_doesNotEchoDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header(HttpHeaders.ORIGIN, DISALLOWED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
