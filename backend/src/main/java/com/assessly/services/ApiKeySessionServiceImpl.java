package com.assessly.services;

import com.assessly.config.AppProperties;
import com.assessly.exceptions.AuthenticationException;
import com.assessly.models.AiProviderSettings;
import com.assessly.repositories.interfaces.AiProviderSettingsRepository;
import com.assessly.services.interfaces.ApiKeySessionService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApiKeySessionServiceImpl implements ApiKeySessionService {
    private final AppProperties properties;
    private final AiProviderSettingsRepository settingsRepository;
    private final Map<UUID, SecretSession> sessions = new ConcurrentHashMap<>();

    public ApiKeySessionServiceImpl(AppProperties properties, AiProviderSettingsRepository settingsRepository) {
        this.properties = properties;
        this.settingsRepository = settingsRepository;
    }

    public ApiSession connect(UUID userId, String providerName, String baseUrl, String modelName, String apiKey) {
        AiProviderSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> new AiProviderSettings(userId, providerName, normalizeBaseUrl(baseUrl), modelName));
        settings.update(providerName, normalizeBaseUrl(baseUrl), modelName);
        validate(settings, apiKey);
        settingsRepository.save(settings);

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(properties.getApiSessionTtlMinutes()));
        sessions.put(userId, new SecretSession(apiKey, settings.getProviderName(), settings.getBaseUrl(), settings.getModelName(), expiresAt));
        return status(userId);
    }

    public ApiSession status(UUID userId) {
        SecretSession session = sessions.get(userId);
        AiProviderSettings settings = settingsRepository.findByUserId(userId).orElse(null);
        if (session == null) {
            return new ApiSession("DISCONNECTED", name(settings), baseUrl(settings), model(settings), null, 0, "AI API key is not connected.");
        }
        long seconds = Duration.between(Instant.now(), session.expiresAt()).toSeconds();
        if (seconds <= 0) {
            sessions.remove(userId);
            return new ApiSession("EXPIRED", session.providerName(), session.baseUrl(), session.modelName(), session.expiresAt(), 0, "API session expired. Please reconnect your AI provider key.");
        }
        return new ApiSession("CONNECTED", session.providerName(), session.baseUrl(), session.modelName(), session.expiresAt(), seconds, "AI API connected.");
    }

    public String requireApiKey(UUID userId) {
        ApiSession status = status(userId);
        if (!"CONNECTED".equals(status.status())) throw new AuthenticationException(status.message());
        return sessions.get(userId).apiKey();
    }

    public void disconnect(UUID userId) {
        sessions.remove(userId);
    }

    public String chatCompletion(UUID userId, List<Map<String, String>> messages) {
        requireApiKey(userId);
        SecretSession session = sessions.get(userId);
        try {
            String response = client().post()
                    .uri(session.baseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", session.modelName(),
                            "messages", messages,
                            "temperature", 0,
                            "response_format", Map.of("type", "json_object")
                    ))
                    .retrieve()
                    .body(String.class);
            JsonNode root = JsonSupport.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) throw new AuthenticationException("Model response format error.");
            return content.asText();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) throw new AuthenticationException("API Key invalid.");
            if (e.getStatusCode().value() == 402) throw new AuthenticationException("API balance is insufficient.");
            if (e.getStatusCode().value() == 429) throw new AuthenticationException("Rate limit from provider.");
            throw new AuthenticationException("Provider rejected the API request: HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new AuthenticationException("Network error or provider timeout.");
        }
    }

    private void validate(AiProviderSettings settings, String apiKey) {
        try {
            client().post()
                    .uri(settings.getBaseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", settings.getModelName(),
                            "messages", List.of(Map.of("role", "user", "content", "Respond with OK.")),
                            "max_tokens", 2
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) throw new AuthenticationException("API Key invalid.");
            if (e.getStatusCode().value() == 402) throw new AuthenticationException("API balance is insufficient.");
            if (e.getStatusCode().value() == 429) throw new AuthenticationException("Rate limit from provider.");
            throw new AuthenticationException("Provider rejected the API request.");
        } catch (ResourceAccessException e) {
            throw new AuthenticationException("Network error or provider timeout.");
        } catch (Exception e) {
            throw new AuthenticationException("Provider unavailable or model response format error.");
        }
    }

    private RestClient client() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private String name(AiProviderSettings settings) { return settings == null ? null : settings.getProviderName(); }
    private String baseUrl(AiProviderSettings settings) { return settings == null ? null : settings.getBaseUrl(); }
    private String model(AiProviderSettings settings) { return settings == null ? null : settings.getModelName(); }

    private record SecretSession(String apiKey, String providerName, String baseUrl, String modelName, Instant expiresAt) {}
}
