package com.cozy.notebooks.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class NotebookDtos {

    private NotebookDtos() {
    }

    public record NotebookResponse(
            UUID id,
            String hrefCode,
            String title,
            String description,
            String color,
            String icon,
            int position,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record CreateNotebookRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 4000) String description,
            @Size(max = 32) String color,
            @Size(max = 64) String icon,
            Integer position
    ) {
    }

    public record UpdateNotebookRequest(
            @Size(max = 255) String title,
            @Size(max = 4000) String description,
            @Size(max = 32) String color,
            @Size(max = 64) String icon,
            Integer position
    ) {
    }
}
