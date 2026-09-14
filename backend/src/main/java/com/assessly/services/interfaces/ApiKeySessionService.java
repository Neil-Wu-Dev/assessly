package com.assessly.services.interfaces;

import java.time.Instant;
import java.util.UUID;

public interface ApiKeySessionService {
    ApiSession connect(UUID userId, String providerName, String baseUrl, String modelName, String apiKey);
    ApiSession status(UUID userId);
    String requireApiKey(UUID userId);

    record ApiSession(String status, String providerName, String baseUrl, String modelName, Instant expiresAt, long remainingSeconds, String message) {}
}
