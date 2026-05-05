package com.cozy.notebooks.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class PageDtos {

    private PageDtos() {
    }

    public record PageResponse(
            UUID id,
            UUID notebookId,
            UUID parentPageId,
            String title,
            String icon,
            String coverUrl,
            int position,
            boolean favorite,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record CreatePageRequest(
            @NotBlank @Size(max = 255) String title,
            UUID parentPageId,
            @Size(max = 64) String icon,
            @Size(max = 1024) String coverUrl,
            Integer position,
            Boolean favorite
    ) {
    }

    public record UpdatePageRequest(
            @Size(max = 255) String title,
            UUID parentPageId,
            @Size(max = 64) String icon,
            @Size(max = 1024) String coverUrl,
            Integer position,
            Boolean favorite
    ) {
    }
}
