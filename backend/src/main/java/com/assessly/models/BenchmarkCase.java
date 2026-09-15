package com.assessly.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "benchmark_cases")
public class BenchmarkCase {
    @Id
    private UUID id;
    @Column(name = "owner_id")
    private UUID ownerId;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(name = "control_text", nullable = false)
    private String controlText;
    @Column(name = "test_data_json", nullable = false)
    private String testDataJson;
    @Column(name = "ground_truth_rule_json", nullable = false)
    private String groundTruthRuleJson;
    @Column(name = "expected_result_json", nullable = false)
    private String expectedResultJson;
    @Column(nullable = false)
    private boolean builtin;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BenchmarkCase() {}

    public BenchmarkCase(UUID ownerId, String name, String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson, boolean builtin) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.name = name;
        this.controlText = controlText;
        this.testDataJson = testDataJson;
        this.groundTruthRuleJson = groundTruthRuleJson;
        this.expectedResultJson = expectedResultJson == null ? "" : expectedResultJson;
        this.builtin = builtin;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getControlText() { return controlText; }
    public String getTestDataJson() { return testDataJson; }
    public String getGroundTruthRuleJson() { return groundTruthRuleJson; }
    public String getExpectedResultJson() { return expectedResultJson; }
    public boolean isBuiltin() { return builtin; }
    public Instant getCreatedAt() { return createdAt; }
}
