package com.taskhive.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 6, max = 128) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record UserSummary(
            UUID id,
            String name,
            String email,
            String role
    ) {
    }

    public record AuthResponse(
            String accessToken,
            UserSummary user
    ) {
    }
}
