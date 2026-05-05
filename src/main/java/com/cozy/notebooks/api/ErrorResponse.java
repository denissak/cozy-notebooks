package com.cozy.notebooks.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(OffsetDateTime.now(), status, code, message, path, null);
    }

    public static ErrorResponse of(int status, String code, String message, String path, List<FieldError> errors) {
        return new ErrorResponse(OffsetDateTime.now(), status, code, message, path, errors);
    }
}
