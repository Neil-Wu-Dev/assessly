package com.assessly.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "benchmark_runs")
public class BenchmarkRun {
    @Id
    private UUID id;
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    @Column(name = "benchmark_case_id")
    private UUID benchmarkCaseId;
    @Column(name = "control_text", nullable = false)
    private String controlText;
    @Column(name = "test_data_json", nullable = false)
    private String testDataJson;
    @Column(name = "ground_truth_rule_json", nullable = false)
    private String groundTruthRuleJson;
    @Column(name = "expected_result_json")
    private String expectedResultJson;
    @Column(name = "ai_generated_rule_json", nullable = false)
    private String aiGeneratedRuleJson;
    @Column(name = "generated_execution_json", nullable = false)
    private String generatedExecutionJson;
    @Column(name = "ground_truth_execution_json", nullable = false)
    private String groundTruthExecutionJson;
    @Column(name = "differences_json", nullable = false)
    private String differencesJson;
    @Column(name = "structurally_equivalent", nullable = false)
    private boolean structurallyEquivalent;
    @Column(name = "execution_equivalent", nullable = false)
    private boolean executionEquivalent;
    @Column(nullable = false)
    private double accuracy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BenchmarkRun() {}

    public BenchmarkRun(UUID ownerId, UUID benchmarkCaseId, String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson, BenchmarkResult result) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.benchmarkCaseId = benchmarkCaseId;
        this.controlText = controlText;
        this.testDataJson = testDataJson;
        this.groundTruthRuleJson = groundTruthRuleJson;
        this.expectedResultJson = expectedResultJson;
        this.aiGeneratedRuleJson = result.getAiGeneratedRuleJson();
        this.generatedExecutionJson = result.getGeneratedExecutionJson();
        this.groundTruthExecutionJson = result.getGroundTruthExecutionJson();
        this.differencesJson = com.assessly.services.JsonSupport.write(result.getDifferences());
        this.structurallyEquivalent = result.isStructurallyEquivalent();
        this.executionEquivalent = result.isExecutionEquivalent();
        this.accuracy = result.getAccuracy();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getBenchmarkCaseId() { return benchmarkCaseId; }
    public String getControlText() { return controlText; }
    public String getTestDataJson() { return testDataJson; }
    public String getGroundTruthRuleJson() { return groundTruthRuleJson; }
    public String getExpectedResultJson() { return expectedResultJson; }
    public String getAiGeneratedRuleJson() { return aiGeneratedRuleJson; }
    public String getGeneratedExecutionJson() { return generatedExecutionJson; }
    public String getGroundTruthExecutionJson() { return groundTruthExecutionJson; }
    public String getDifferencesJson() { return differencesJson; }
    public boolean isStructurallyEquivalent() { return structurallyEquivalent; }
    public boolean isExecutionEquivalent() { return executionEquivalent; }
    public double getAccuracy() { return accuracy; }
    public Instant getCreatedAt() { return createdAt; }
}
