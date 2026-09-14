package com.assessly.schemas;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class AuthSchemas {
    public record RegisterRequest(@Email @NotBlank String email, @Size(min = 8) String password) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(UUID userId, String email, String sessionToken) {}
    public record ApiKeyRequest(@NotBlank String providerName, @NotBlank String baseUrl, @NotBlank String modelName, @NotBlank String apiKey) {}
    public record ApiSessionResponse(String status, String providerName, String baseUrl, String modelName, Instant expiresAt, long remainingSeconds, String message) {}
}
