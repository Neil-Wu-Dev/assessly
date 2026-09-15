package com.assessly.repositories.interfaces;

import com.assessly.models.AssessmentRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository {
    AssessmentRun save(AssessmentRun assessmentRun);
    List<AssessmentRun> findByDataset(UUID datasetId);
    Optional<AssessmentRun> findByIdAndDataset(UUID assessmentId, UUID datasetId);
}
