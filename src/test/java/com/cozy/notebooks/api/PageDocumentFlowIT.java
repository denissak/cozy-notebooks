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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the page-document API treats {@code content} as opaque JSON:
 * deeply nested objects/arrays and arbitrary extra fields round-trip through
 * MySQL's {@code JSON} column unchanged, full-content PUT replaces the entire
 * document (not merge), and {@code version}/{@code contentHash} update on
 * each successful write.
 */
class PageDocumentFlowIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void pageContent_withDeeplyNestedJson_roundTripsExactly() throws Exception {
        UUID notebookId = createNotebook("Round-trip");

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("version", 1);
        content.put("blocks", List.of(
                Map.of("id", "b1", "type", "paragraph",
                        "content", Map.of("text", "hello", "language", "en"),
                        "extra", Map.of("source", "import",
                                "tags", List.of("draft", "todo"),
                                "score", 0.42)),
                Map.of("id", "b2", "type", "checklist",
                        "content", Map.of(
                                "items", List.of(
                                        Map.of("text", "task", "checked", false,
                                                "metadata", Map.of("priority", 1,
                                                        "labels", List.of("a", "b"))),
                                        Map.of("text", "task 2", "checked", true,
                                                "subItems", List.of(
                                                        Map.of("text", "sub", "checked", false)
                                                ))
                                )
                        ))
        ));
        content.put("metadata", Map.of("createdBy", "alice", "schema", "1.0"));

        UUID pageId = createPage(notebookId, "Nested page", content);

        MvcResult res = mockMvc.perform(get("/api/v1/pages/{pid}", pageId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsString());

        assertThat(body.get("content"))
                .isEqualTo(objectMapper.valueToTree(content));
    }

    @Test
    void putPage_replacesContentExactly_andBumpsVersionAndHash() throws Exception {
        UUID notebookId = createNotebook("Replace");

        Map<String, Object> initial = Map.of(
                "version", 1,
                "blocks", List.of(
                        Map.of("id", "b1", "type", "paragraph",
                                "content", Map.of("text", "old"))
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
        long versionV1 = created.get("version").asLong();
        String hashV1 = created.get("contentHash").asText();
        assertThat(versionV1).isEqualTo(1L);

        Map<String, Object> reshaped = Map.of(
                "version", 1,
                "blocks", List.of(
                        Map.of("id", "h1", "type", "heading",
                                "content", Map.of("text", "Section", "level", 2)),
                        Map.of("id", "i1", "type", "image",
                                "content", Map.of("url", "https://example.com/x.png",
                                        "width", 1024, "height", 768))
                ),
                "metadata", Map.of("imported", true)
        );

        MvcResult putRes = mockMvc.perform(put("/api/v1/pages/{pid}", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "baseHash", hashV1,
                                "content", reshaped))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode updated = objectMapper.readTree(putRes.getResponse().getContentAsString());
        long versionV2 = updated.get("version").asLong();
        String hashV2 = updated.get("contentHash").asText();

        assertThat(versionV2).isEqualTo(2L);
        assertThat(hashV2).isNotEqualTo(hashV1);
        assertThat(updated.get("content"))
                .isEqualTo(objectMapper.valueToTree(reshaped));

        MvcResult readBackRes = mockMvc.perform(get("/api/v1/pages/{pid}", pageId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode readBack = objectMapper.readTree(readBackRes.getResponse().getContentAsString());
        assertThat(readBack.get("contentHash").asText()).isEqualTo(hashV2);
        assertThat(readBack.get("version").asLong()).isEqualTo(2L);
        assertThat(readBack.get("content"))
                .isEqualTo(objectMapper.valueToTree(reshaped));
    }

    @Test
    void putPage_withoutBaseHash_isAllowed() throws Exception {
        UUID notebookId = createNotebook("No baseHash");
        UUID pageId = createPage(notebookId, "P", Map.of("blocks", List.of()));

        mockMvc.perform(put("/api/v1/pages/{pid}", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("blocks", List.of(
                                        Map.of("id", "b1", "type", "paragraph",
                                                "content", Map.of("text", "hi"))
                                ))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void listPagesInNotebook_returnsCreatedPage() throws Exception {
        UUID notebookId = createNotebook("List test");
        UUID pageA = createPage(notebookId, "Alpha", Map.of("blocks", List.of()));
        UUID pageB = createPage(notebookId, "Beta", Map.of("blocks", List.of()));

        mockMvc.perform(get("/api/v1/notebooks/{nid}/pages", notebookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id=='" + pageA + "')].title").value("Alpha"))
                .andExpect(jsonPath("$[?(@.id=='" + pageB + "')].title").value("Beta"));
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

    private UUID createPage(UUID notebookId, String title, Map<String, Object> content) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/notebooks/{nid}/pages", notebookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title, "content", content))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString())
                .get("id").asText());
    }
}
