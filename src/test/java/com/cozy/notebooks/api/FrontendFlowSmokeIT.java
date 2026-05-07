package com.cozy.notebooks.api;

import com.cozy.notebooks.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end smoke flow against the page-document API. Mirrors the typical
 * front-end workflow: create notebook → create page with full content document
 * → fetch page → replace content via PUT (content_hash + version change) →
 * soft-delete → verify page is gone.
 */
class FrontendFlowSmokeIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void mainFrontendFlow_smoke() throws Exception {
        UUID notebookId = createNotebook("Smoke Notebook");

        Map<String, Object> initialContent = Map.of(
                "version", 1,
                "blocks", List.of(
                        Map.of("id", "b1", "type", "paragraph",
                                "content", Map.of("text", "Paragraph text")),
                        Map.of("id", "b2", "type", "checklist",
                                "content", Map.of("items", List.of(
                                        Map.of("text", "Item 1", "checked", false),
                                        Map.of("text", "Item 2", "checked", true)
                                )))
                )
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/notebooks/{nid}/pages", notebookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Smoke Page",
                                "content", initialContent))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Smoke Page"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.contentHash").isString())
                .andExpect(jsonPath("$.content.version").value(1))
                .andExpect(jsonPath("$.content.blocks.length()").value(2))
                .andReturn();

        JsonNode created = objectMapper.readTree(createRes.getResponse().getContentAsString());
        UUID pageId = UUID.fromString(created.get("id").asText());
        String hashV1 = created.get("contentHash").asText();

        mockMvc.perform(get("/api/v1/pages/{pid}", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pageId.toString()))
                .andExpect(jsonPath("$.notebookId").value(notebookId.toString()))
                .andExpect(jsonPath("$.title").value("Smoke Page"))
                .andExpect(jsonPath("$.content.blocks[0].type").value("paragraph"))
                .andExpect(jsonPath("$.content.blocks[1].type").value("checklist"))
                .andExpect(jsonPath("$.contentHash").value(hashV1))
                .andExpect(jsonPath("$.version").value(1));

        Map<String, Object> updatedContent = new LinkedHashMap<>();
        updatedContent.put("version", 1);
        updatedContent.put("blocks", List.of(
                Map.of("id", "b1", "type", "paragraph",
                        "content", Map.of("text", "Paragraph updated")),
                Map.of("id", "b3", "type", "heading",
                        "content", Map.of("text", "New section", "level", 2))
        ));

        MvcResult putRes = mockMvc.perform(put("/api/v1/pages/{pid}", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Smoke Page (v2)",
                                "baseHash", hashV1,
                                "content", updatedContent))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Smoke Page (v2)"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.content.blocks[0].content.text").value("Paragraph updated"))
                .andExpect(jsonPath("$.content.blocks[1].type").value("heading"))
                .andReturn();

        String hashV2 = objectMapper.readTree(putRes.getResponse().getContentAsString())
                .get("contentHash").asText();
        if (hashV1.equals(hashV2)) {
            throw new AssertionError("contentHash should change after content replacement");
        }

        mockMvc.perform(delete("/api/v1/pages/{pid}", pageId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/pages/{pid}", pageId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));

        mockMvc.perform(put("/api/v1/pages/{pid}", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("blocks", List.of())))))
                .andExpect(status().isNotFound());
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
