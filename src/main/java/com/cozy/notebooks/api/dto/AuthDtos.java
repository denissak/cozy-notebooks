package com.cozy.notebooks.api.dto;

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

    public record AuthUserResponse(
            UUID id,
            String email
    ) {
    }

    public record AuthTokensResponse(
            String accessToken,
            String refreshToken,
            AuthUserResponse user
    ) {
    }

    public record RefreshTokensResponse(
            String accessToken,
            String refreshToken
    ) {
    }

    public record MeResponse(
            UUID id,
            String email
    ) {
    }
}
