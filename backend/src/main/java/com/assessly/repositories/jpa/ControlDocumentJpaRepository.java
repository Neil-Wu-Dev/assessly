package com.assessly.repositories.jpa;

import com.assessly.models.ControlDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ControlDocumentJpaRepository extends JpaRepository<ControlDocument, UUID> {
    List<ControlDocument> findByDatasetIdOrderByCreatedAtDesc(UUID datasetId);
    Optional<ControlDocument> findByIdAndDatasetId(UUID id, UUID datasetId);
}
