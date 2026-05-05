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

class FrontendFlowSmokeIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void mainFrontendFlow_smoke() throws Exception {
        // 1) Create notebook
        UUID notebookId = createNotebook("Smoke Notebook");

        // 2) Create page under notebook
        UUID pageId = createPage(notebookId, "Smoke Page");

        // 3) Create paragraph block under page
        UUID paragraphBlockId = createBlock(pageId, "paragraph", Map.of("text", "Paragraph text"), 0);

        // 4) Create checklist block under page
        UUID checklistBlockId = createBlock(pageId, "checklist",
                Map.of("items", List.of(
                        Map.of("text", "Item 1", "checked", false),
                        Map.of("text", "Item 2", "checked", true)
                )), 1);

        // 5) Get page by id, and verify page exists + blocks are returned ordered by position
        mockMvc.perform(get("/api/v1/pages/{pageId}", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pageId.toString()))
                .andExpect(jsonPath("$.notebookId").value(notebookId.toString()))
                .andExpect(jsonPath("$.title").value("Smoke Page"));

        mockMvc.perform(get("/api/v1/pages/{pageId}/blocks", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(paragraphBlockId.toString()))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].id").value(checklistBlockId.toString()))
                .andExpect(jsonPath("$[1].position").value(1));

        // 6) Patch one block content
        mockMvc.perform(patch("/api/v1/blocks/{blockId}", paragraphBlockId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("text", "Paragraph updated")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paragraphBlockId.toString()))
                .andExpect(jsonPath("$.content.text").value("Paragraph updated"));

        // 7) Reorder blocks
        mockMvc.perform(post("/api/v1/pages/{pageId}/blocks/reorder", pageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "blockIds", List.of(checklistBlockId, paragraphBlockId)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(checklistBlockId.toString()))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].id").value(paragraphBlockId.toString()))
                .andExpect(jsonPath("$[1].position").value(1));

        // 8) Soft delete one block
        mockMvc.perform(delete("/api/v1/blocks/{blockId}", checklistBlockId))
                .andExpect(status().isNoContent());

        // 9) Verify deleted block is not returned
        mockMvc.perform(get("/api/v1/pages/{pageId}/blocks", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(paragraphBlockId.toString()));

        mockMvc.perform(patch("/api/v1/blocks/{blockId}", checklistBlockId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("text", "should fail")
                        ))))
                .andExpect(status().isNotFound());
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
        MvcResult res = mockMvc.perform(post("/api/v1/notebooks/{notebookId}/pages", notebookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(res);
    }

    private UUID createBlock(UUID pageId, String type, Map<String, Object> content, int position) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/pages/{pageId}/blocks", pageId)
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
