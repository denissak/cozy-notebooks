package com.cozy.notebooks.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class PageTemplateDtos {

    private PageTemplateDtos() {
    }

    public record TemplateResponse(
            UUID id,
            String hrefCode,
            String name,
            String description,
            String icon,
            JsonNode content,
            String contentHash,
            boolean builtIn,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record CreateTemplateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 4000) String description,
            @Size(max = 64) String icon,
            @NotNull JsonNode content
    ) {
    }

    public record UpdateTemplateRequest(
            @Size(max = 255) String name,
            @Size(max = 4000) String description,
            @Size(max = 64) String icon,
            JsonNode content
    ) {
    }

    public record CreatePageFromTemplateRequest(
            @NotNull UUID notebookId,
            @Size(max = 255) String title
    ) {
    }
}
