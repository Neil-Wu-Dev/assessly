package com.assessly.repositories;

import com.assessly.models.AssessmentRun;
import com.assessly.repositories.interfaces.AssessmentRepository;
import com.assessly.repositories.jpa.AssessmentJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class AssessmentRepositoryAdapter implements AssessmentRepository {
    private final AssessmentJpaRepository jpa;

    AssessmentRepositoryAdapter(AssessmentJpaRepository jpa) {
        this.jpa = jpa;
    }

    public AssessmentRun save(AssessmentRun assessmentRun) { return jpa.save(assessmentRun); }
    public List<AssessmentRun> findByDataset(UUID datasetId) { return jpa.findByDatasetIdOrderByCreatedAtDesc(datasetId); }
}
