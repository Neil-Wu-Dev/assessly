package com.assessly.schemas;

import com.assessly.defs.AssessmentStatus;

import java.time.Instant;
import java.util.UUID;

public class AssessmentSchemas {
    public record StartAssessmentRequest(UUID evidenceFileId, UUID ruleSetId) {}
    public record AssessmentRunResponse(UUID id, UUID datasetId, UUID ruleSetId, UUID evidenceFileId, AssessmentStatus status, int recordsEvaluated, int rulesEvaluated, int violationsDetected, String resultJson, Instant createdAt) {}
}
