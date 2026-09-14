package com.assessly.models;

import com.assessly.defs.RuleGenerationStatus;
import com.assessly.exceptions.RuleValidationException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rule_sets")
public class RuleSet {
    @Id
    private UUID id;
    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;
    @Column(nullable = false)
    private int version;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleGenerationStatus status;
    @Column(name = "rules_json", nullable = false)
    private String rulesJson;
    @Column(name = "generation_summary_json", nullable = false)
    private String generationSummaryJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected RuleSet() {
    }

    public RuleSet(UUID datasetId, int version, RuleGenerationStatus status, String rulesJson, String generationSummaryJson) {
        this.id = UUID.randomUUID();
        this.datasetId = datasetId;
        this.version = version;
        this.status = status;
        this.rulesJson = rulesJson;
        this.generationSummaryJson = generationSummaryJson;
        this.createdAt = Instant.now();
    }

    public void confirmExecutable() {
        if (status != RuleGenerationStatus.READY) {
            throw new RuleValidationException("Only READY rule sets can be confirmed for execution.");
        }
        this.confirmedAt = Instant.now();
    }

    public boolean isExecutable() {
        return status == RuleGenerationStatus.READY && confirmedAt != null;
    }

    public UUID getId() { return id; }
    public UUID getDatasetId() { return datasetId; }
    public int getVersion() { return version; }
    public RuleGenerationStatus getStatus() { return status; }
    public String getRulesJson() { return rulesJson; }
    public String getGenerationSummaryJson() { return generationSummaryJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
