package com.assessly.repositories.jpa;

import com.assessly.models.AssessmentRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentJpaRepository extends JpaRepository<AssessmentRun, UUID> {
    List<AssessmentRun> findByDatasetIdOrderByCreatedAtDesc(UUID datasetId);
}
