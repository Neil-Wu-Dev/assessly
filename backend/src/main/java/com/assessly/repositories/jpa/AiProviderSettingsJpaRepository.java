package com.assessly.repositories.jpa;

import com.assessly.models.AiProviderSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiProviderSettingsJpaRepository extends JpaRepository<AiProviderSettings, UUID> {
    Optional<AiProviderSettings> findByUserId(UUID userId);
}
