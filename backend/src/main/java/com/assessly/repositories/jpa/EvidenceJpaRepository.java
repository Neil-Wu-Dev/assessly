package com.assessly.repositories.jpa;

import com.assessly.models.EvidenceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceJpaRepository extends JpaRepository<EvidenceFile, UUID> {
    List<EvidenceFile> findByDatasetIdOrderByCreatedAtDesc(UUID datasetId);
    Optional<EvidenceFile> findByIdAndDatasetId(UUID id, UUID datasetId);
}
