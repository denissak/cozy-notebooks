package com.cozy.notebooks.api.dto;

import com.cozy.notebooks.domain.BlockType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class BlockDtos {

    private BlockDtos() {
    }

    public record BlockResponse(
            UUID id,
            UUID notebookId,
            UUID pageId,
            UUID parentBlockId,
            BlockType type,
            JsonNode content,
            JsonNode settings,
            int position,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record CreateBlockRequest(
            @NotNull BlockType type,
            UUID parentBlockId,
            JsonNode content,
            JsonNode settings,
            Integer position
    ) {
    }

    public record UpdateBlockRequest(
            BlockType type,
            UUID parentBlockId,
            JsonNode content,
            JsonNode settings,
            Integer position
    ) {
    }

    public record ReorderRequest(
            @NotEmpty List<@NotNull UUID> blockIds
    ) {
    }
}
