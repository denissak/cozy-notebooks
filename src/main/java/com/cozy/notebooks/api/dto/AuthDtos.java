package com.cozy.notebooks.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 256) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 256) String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record LogoutRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record GoogleLoginRequest(
            @NotBlank String idToken
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record AuthUserResponse(
            UUID id,
            String email,
            @JsonInclude(JsonInclude.Include.ALWAYS) String avatarUrl
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record AuthTokensResponse(
            String accessToken,
            String refreshToken,
            @JsonInclude(JsonInclude.Include.ALWAYS) AuthUserResponse user
    ) {
    }

    public record RefreshTokensResponse(
            String accessToken,
            String refreshToken
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MeResponse(
            UUID id,
            String email,
            @JsonInclude(JsonInclude.Include.ALWAYS) String avatarUrl
    ) {
    }
}
