package com.cozy.notebooks.api;

import com.cozy.notebooks.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotebookControllerIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void create_get_update_delete_notebook() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "My Diary",
                "description", "Cozy diary",
                "color", "#fcd34d",
                "icon", "book",
                "position", 1
        );

        MvcResult result = mockMvc.perform(post("/api/v1/notebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("My Diary"))
                .andExpect(jsonPath("$.position").value(1))
                .andReturn();

        UUID id = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/notebooks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Diary"));

        mockMvc.perform(patch("/api/v1/notebooks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"));

        mockMvc.perform(get("/api/v1/notebooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]", hasSize(1)));

        mockMvc.perform(delete("/api/v1/notebooks/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notebooks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void create_notebook_validation_returnsFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/notebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void get_unknownNotebook_returns404WithUnifiedError() throws Exception {
        mockMvc.perform(get("/api/v1/notebooks/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidUuidPathParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/notebooks/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"));
    }
}
