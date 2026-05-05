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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PageBlockFlowIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void fullFlow_createNotebookPageBlocks_andReorder() throws Exception {
        UUID notebookId = createNotebook("Lab notes");
        UUID pageId = createPage(notebookId, "Day 1");

        UUID b1 = createBlock(pageId, "paragraph", Map.of("text", "Hello world"), 0);
        UUID b2 = createBlock(pageId, "heading", Map.of("text", "Section A", "level", 2), 1);
        UUID b3 = createBlock(pageId, "todo", Map.of("text", "Buy milk", "done", false), 2);

        mockMvc.perform(get("/api/v1/pages/{pid}/blocks", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("paragraph"))
                .andExpect(jsonPath("$[2].type").value("todo"));

        mockMvc.perform(patch("/api/v1/blocks/{bid}", b1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("text", "Updated text")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.text").value("Updated text"));

        mockMvc.perform(post("/api/v1/pages/{pid}/blocks/reorder", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "blockIds", List.of(b3, b1, b2)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(b3.toString()))
                .andExpect(jsonPath("$[1].id").value(b1.toString()))
                .andExpect(jsonPath("$[2].id").value(b2.toString()));

        mockMvc.perform(delete("/api/v1/blocks/{bid}", b2))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/pages/{pid}/blocks", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void createBlock_unknownType_returns400() throws Exception {
        UUID notebookId = createNotebook("Misc");
        UUID pageId = createPage(notebookId, "Notes");

        mockMvc.perform(post("/api/v1/pages/{pid}/blocks", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"banana\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("malformed_request"));
    }

    @Test
    void createBlock_missingType_returnsValidationError() throws Exception {
        UUID notebookId = createNotebook("Misc 2");
        UUID pageId = createPage(notebookId, "Notes");

        mockMvc.perform(post("/api/v1/pages/{pid}/blocks", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("type"));
    }

    @Test
    void reorder_withMismatchedIds_returns400() throws Exception {
        UUID notebookId = createNotebook("Reorder failures");
        UUID pageId = createPage(notebookId, "Page");
        createBlock(pageId, "paragraph", Map.of("text", "a"), 0);
        createBlock(pageId, "paragraph", Map.of("text", "b"), 1);

        mockMvc.perform(post("/api/v1/pages/{pid}/blocks/reorder", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "blockIds", List.of(UUID.randomUUID())
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"));
    }

    private UUID createNotebook(String title) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/notebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(res);
    }

    private UUID createPage(UUID notebookId, String title) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/notebooks/{nid}/pages", notebookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(res);
    }

    private UUID createBlock(UUID pageId, String type, Map<String, Object> content, int position) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/pages/{pid}/blocks", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", type,
                                "content", content,
                                "position", position
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(res);
    }

    private UUID readId(MvcResult res) throws Exception {
        JsonNode tree = objectMapper.readTree(res.getResponse().getContentAsString());
        return UUID.fromString(tree.get("id").asText());
    }
}
