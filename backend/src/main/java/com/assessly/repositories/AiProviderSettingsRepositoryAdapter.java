package com.assessly.repositories;

import com.assessly.models.AiProviderSettings;
import com.assessly.repositories.interfaces.AiProviderSettingsRepository;
import com.assessly.repositories.jpa.AiProviderSettingsJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class AiProviderSettingsRepositoryAdapter implements AiProviderSettingsRepository {
    private final AiProviderSettingsJpaRepository jpa;

    AiProviderSettingsRepositoryAdapter(AiProviderSettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    public AiProviderSettings save(AiProviderSettings settings) {
        return jpa.save(settings);
    }

    public Optional<AiProviderSettings> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }
}
