package com.cozy.notebooks.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class FeedbackDtos {

    private FeedbackDtos() {
    }

    public record CreateFeedbackRequest(
            @NotBlank
            @Pattern(regexp = "^(bug|idea|question|other)$",
                    message = "type must be one of: bug, idea, question, other")
            String type,
            @NotBlank
            @Size(max = 5000)
            String message
    ) {
    }

    public record FeedbackResponse(
            UUID id,
            String type,
            String message,
            String status,
            OffsetDateTime createdAt
    ) {
    }
}
