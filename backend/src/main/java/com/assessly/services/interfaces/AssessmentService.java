package com.assessly.services.interfaces;

import com.assessly.models.AssessmentRun;

import java.util.List;
import java.util.UUID;

public interface AssessmentService {
    AssessmentRun start(UUID ownerId, UUID datasetId, UUID evidenceFileId, UUID ruleSetId);
    List<AssessmentRun> history(UUID ownerId, UUID datasetId);
    AssessmentRun get(UUID ownerId, UUID datasetId, UUID assessmentId);
}
