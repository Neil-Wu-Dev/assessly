package com.assessly.repositories.jpa;

import com.assessly.models.AssessmentRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentJpaRepository extends JpaRepository<AssessmentRun, UUID> {
    List<AssessmentRun> findByDatasetIdOrderByCreatedAtDesc(UUID datasetId);
    Optional<AssessmentRun> findByIdAndDatasetId(UUID id, UUID datasetId);
}
