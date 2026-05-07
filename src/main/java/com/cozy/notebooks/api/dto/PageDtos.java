package com.cozy.notebooks.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class PageDtos {

    private PageDtos() {
    }

    /**
     * Full page document. {@code content} is the entire page JSON tree
     * (typically {@code {"version": N, "blocks": [...]}}, but the backend
     * does not enforce a specific shape).
     */
    public record PageResponse(
            UUID id,
            UUID notebookId,
            String title,
            JsonNode content,
            String contentHash,
            long version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record CreatePageRequest(
            @NotBlank @Size(max = 255) String title,
            @NotNull JsonNode content
    ) {
    }

    /**
     * Full-content replace. {@code baseHash} is optional — when present, the
     * server compares it to the page's current {@code contentHash} and rejects
     * the update with HTTP 409 if they differ.
     */
    public record UpdatePageRequest(
            @Size(max = 255) String title,
            String baseHash,
            @NotNull JsonNode content
    ) {
    }
}
