package com.assessly.mappers;

import com.assessly.models.AssessmentRun;
import com.assessly.schemas.AssessmentSchemas.AssessmentRunResponse;
import org.springframework.stereotype.Component;

@Component
public class AssessmentMapper {
    public AssessmentRunResponse toResponse(AssessmentRun run) {
        return new AssessmentRunResponse(run.getId(), run.getDatasetId(), run.getRuleSetId(), run.getEvidenceFileId(), run.getStatus(), run.getRecordsEvaluated(), run.getRulesEvaluated(), run.getViolationsDetected(), run.getResultJson(), run.getCreatedAt());
    }
}
