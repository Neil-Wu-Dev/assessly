package com.assessly.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_provider_settings")
public class AiProviderSettings {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    @Column(name = "provider_name", nullable = false, length = 80)
    private String providerName;
    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;
    @Column(name = "model_name", nullable = false, length = 160)
    private String modelName;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiProviderSettings() {
    }

    public AiProviderSettings(UUID userId, String providerName, String baseUrl, String modelName) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        update(providerName, baseUrl, modelName);
    }

    public void update(String providerName, String baseUrl, String modelName) {
        if (providerName == null || providerName.isBlank()) throw new IllegalArgumentException("AI provider name is required.");
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("AI provider base URL is required.");
        if (modelName == null || modelName.isBlank()) throw new IllegalArgumentException("AI model name is required.");
        this.providerName = providerName.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.modelName = modelName.trim();
        this.updatedAt = Instant.now();
    }

    private String normalizeBaseUrl(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            throw new IllegalArgumentException("AI provider base URL must start with http:// or https://.");
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getProviderName() { return providerName; }
    public String getBaseUrl() { return baseUrl; }
    public String getModelName() { return modelName; }
    public Instant getUpdatedAt() { return updatedAt; }
}
