package com.assessly.repositories.interfaces;

import com.assessly.models.AiProviderSettings;

import java.util.Optional;
import java.util.UUID;

public interface AiProviderSettingsRepository {
    AiProviderSettings save(AiProviderSettings settings);
    Optional<AiProviderSettings> findByUserId(UUID userId);
}
