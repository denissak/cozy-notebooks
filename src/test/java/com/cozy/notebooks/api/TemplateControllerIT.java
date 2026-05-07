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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TemplateControllerIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createTemplate_andInstantiatePage_copiesContent() throws Exception {
        MvcResult notebookRes = mockMvc.perform(post("/api/v1/notebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Templates target\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID notebookId = UUID.fromString(objectMapper.readTree(notebookRes.getResponse()
                .getContentAsString()).get("id").asText());

        Map<String, Object> templateContent = Map.of(
                "version", 1,
                "blocks", List.of(
                        Map.of("id", "h1", "type", "heading",
                                "content", Map.of("text", "Today")),
                        Map.of("id", "p1", "type", "paragraph",
                                "content", Map.of("text", "...")),
                        Map.of("id", "t1", "type", "todo",
                                "content", Map.of("text", "Item", "done", false))
                )
        );

        MvcResult tres = mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Daily journal",
                                "icon", "calendar",
                                "content", templateContent))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Daily journal"))
                .andExpect(jsonPath("$.contentHash").isString())
                .andExpect(jsonPath("$.content.blocks.length()").value(3))
                .andReturn();
        UUID templateId = UUID.fromString(objectMapper.readTree(tres.getResponse()
                .getContentAsString()).get("id").asText());

        MvcResult pageRes = mockMvc.perform(post("/api/v1/templates/{tid}/create-page", templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "notebookId", notebookId.toString(),
                                "title", "Today's entry"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Today's entry"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn();
        JsonNode pageBody = objectMapper.readTree(pageRes.getResponse().getContentAsString());
        UUID pageId = UUID.fromString(pageBody.get("id").asText());

        assertThat(pageBody.get("content"))
                .isEqualTo(objectMapper.valueToTree(templateContent));

        mockMvc.perform(get("/api/v1/pages/{pid}", pageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.blocks[0].type").value("heading"))
                .andExpect(jsonPath("$.content.blocks[2].type").value("todo"));

        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + templateId + "')].name").value("Daily journal"));
    }

    @Test
    void createTemplate_validationFailsWhenNameMissing() throws Exception {
        mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"content\":{\"blocks\":[]}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void createTemplate_validationFailsWhenContentMissing() throws Exception {
        mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"empty\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.errors[0].field").value("content"));
    }
}
