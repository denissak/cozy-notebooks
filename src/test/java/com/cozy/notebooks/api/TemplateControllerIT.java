package com.cozy.notebooks.api;

import com.cozy.notebooks.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TemplateControllerIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createTemplate_andInstantiatePage_writesBlocks() throws Exception {
        // Notebook to host the new page
        MvcResult notebook = mockMvc.perform(post("/api/v1/notebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Templates target\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID notebookId = UUID.fromString(objectMapper.readTree(notebook.getResponse().getContentAsString()).get("id").asText());

        Map<String, Object> tmplBody = Map.of(
                "name", "Daily journal",
                "icon", "calendar",
                "blocks", List.of(
                        Map.of("type", "heading", "content", Map.of("text", "Today"), "position", 0),
                        Map.of("type", "paragraph", "content", Map.of("text", "..."), "position", 1),
                        Map.of("type", "todo", "content", Map.of("text", "Item", "done", false), "position", 2)
                )
        );

        MvcResult tres = mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tmplBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Daily journal"))
                .andExpect(jsonPath("$.blocks.length()").value(3))
                .andReturn();
        UUID templateId = UUID.fromString(objectMapper.readTree(tres.getResponse().getContentAsString()).get("id").asText());

        MvcResult pageRes = mockMvc.perform(post("/api/v1/templates/{tid}/create-page", templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "notebookId", notebookId.toString(),
                                "title", "Today's entry"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Today's entry"))
                .andReturn();
        UUID pageId = UUID.fromString(objectMapper.readTree(pageRes.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/pages/{pid}/blocks", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("heading"))
                .andExpect(jsonPath("$[2].type").value("todo"));

        // List templates contains the new one
        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + templateId + "')].name").value("Daily journal"));
    }

    @Test
    void createTemplate_validationFailsWhenNameMissing() throws Exception {
        mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }
}
