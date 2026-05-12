package com.cozy.notebooks.api;

import com.cozy.notebooks.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies optimistic-concurrency conflict detection on page updates:
 * a stale {@code baseHash} returns HTTP 409 and leaves the page unchanged
 * in the database. A subsequent update with the correct hash succeeds.
 */
class PageHashConflictIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void putPage_withWrongBaseHash_returns409_andDbIsUnchanged() throws Exception {
        UUID notebookId = createNotebook("Conflict");

        Map<String, Object> initial = Map.of(
                "version", 1,
                "blocks", List.of(
                        Map.of("id", "b1", "type", "paragraph",
                                "content", Map.of("text", "original"))
                )
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/notebooks/{nid}/pages", notebookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "P", "content", initial))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        UUID pageId = UUID.fromString(created.get("id").asText());
        String currentHash = created.get("contentHash").asText();
        long currentVersion = created.get("version").asLong();

        Map<String, Object> attempted = Map.of(
                "version", 1,
                "blocks", List.of(
                        Map.of("id", "b1", "type", "paragraph",
                                "content", Map.of("text", "stale-write"))
                )
        );

        String staleHash = "0".repeat(64);
        mockMvc.perform(put("/api/v1/pages/{pid}", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "baseHash", staleHash,
                                "content", attempted))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("conflict"))
                .andExpect(jsonPath("$.message").value("Page content was modified by another update"));

        MvcResult readRes = mockMvc.perform(get("/api/v1/pages/{pid}", pageId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode readBack = objectMapper.readTree(readRes.getResponse().getContentAsString());

        assertThat(readBack.get("contentHash").asText()).isEqualTo(currentHash);
        assertThat(readBack.get("version").asLong()).isEqualTo(currentVersion);
        assertThat(readBack.get("content"))
                .isEqualTo(objectMapper.valueToTree(initial));

        Map<String, Object> good = Map.of(
                "version", 1,
                "blocks", List.of(
                        Map.of("id", "b1", "type", "paragraph",
                                "content", Map.of("text", "fresh-write"))
                )
        );

        MvcResult goodRes = mockMvc.perform(put("/api/v1/pages/{pid}", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "baseHash", currentHash,
                                "content", good))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode goodBody = objectMapper.readTree(goodRes.getResponse().getContentAsString());

        assertThat(goodBody.get("version").asLong()).isEqualTo(currentVersion + 1);
        assertThat(goodBody.get("contentHash").asText()).isNotEqualTo(currentHash);
        assertThat(goodBody.get("content"))
                .isEqualTo(objectMapper.valueToTree(good));
    }

    private UUID createNotebook(String title) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/notebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString())
                .get("id").asText());
    }
}
