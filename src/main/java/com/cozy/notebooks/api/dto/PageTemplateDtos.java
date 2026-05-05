package com.cozy.notebooks.api.dto;

import com.cozy.notebooks.domain.BlockType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class PageTemplateDtos {

    private PageTemplateDtos() {
    }

    public record TemplateBlock(
            @NotNull BlockType type,
            JsonNode content,
            JsonNode settings,
            Integer position
    ) {
    }

    public record TemplateResponse(
            UUID id,
            String name,
            String description,
            String icon,
            JsonNode blocks,
            boolean builtIn,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record CreateTemplateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 4000) String description,
            @Size(max = 64) String icon,
            @Valid List<TemplateBlock> blocks
    ) {
    }

    public record UpdateTemplateRequest(
            @Size(max = 255) String name,
            @Size(max = 4000) String description,
            @Size(max = 64) String icon,
            @Valid List<TemplateBlock> blocks
    ) {
    }

    public record CreatePageFromTemplateRequest(
            @NotNull UUID notebookId,
            @Size(max = 255) String title,
            UUID parentPageId
    ) {
    }
}
