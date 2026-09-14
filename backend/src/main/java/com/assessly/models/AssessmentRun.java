package com.assessly.models;

import com.assessly.defs.AssessmentStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_runs")
public class AssessmentRun {
    @Id
    private UUID id;
    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;
    @Column(name = "rule_set_id", nullable = false)
    private UUID ruleSetId;
    @Column(name = "evidence_file_id", nullable = false)
    private UUID evidenceFileId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentStatus status;
    @Column(name = "records_evaluated", nullable = false)
    private int recordsEvaluated;
    @Column(name = "rules_evaluated", nullable = false)
    private int rulesEvaluated;
    @Column(name = "violations_detected", nullable = false)
    private int violationsDetected;
    @Column(name = "result_json", nullable = false)
    private String resultJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AssessmentRun() {
    }

    public AssessmentRun(UUID datasetId, UUID ruleSetId, UUID evidenceFileId, AssessmentStatus status, int recordsEvaluated, int rulesEvaluated, int violationsDetected, String resultJson) {
        this.id = UUID.randomUUID();
        this.datasetId = datasetId;
        this.ruleSetId = ruleSetId;
        this.evidenceFileId = evidenceFileId;
        this.status = status;
        this.recordsEvaluated = recordsEvaluated;
        this.rulesEvaluated = rulesEvaluated;
        this.violationsDetected = violationsDetected;
        this.resultJson = resultJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDatasetId() { return datasetId; }
    public UUID getRuleSetId() { return ruleSetId; }
    public UUID getEvidenceFileId() { return evidenceFileId; }
    public AssessmentStatus getStatus() { return status; }
    public int getRecordsEvaluated() { return recordsEvaluated; }
    public int getRulesEvaluated() { return rulesEvaluated; }
    public int getViolationsDetected() { return violationsDetected; }
    public String getResultJson() { return resultJson; }
    public Instant getCreatedAt() { return createdAt; }
}
