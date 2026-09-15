package com.assessly.models;

import java.util.List;
import java.util.Map;

public class BenchmarkResult {
    private final boolean structurallyEquivalent;
    private final boolean executionEquivalent;
    private final int totalCases;
    private final int correctCases;
    private final double accuracy;
    private final String aiGeneratedRuleJson;
    private final String generatedExecutionJson;
    private final String groundTruthExecutionJson;
    private final List<Map<String, Object>> differences;

    public BenchmarkResult(boolean structurallyEquivalent, boolean executionEquivalent, String aiGeneratedRuleJson, String generatedExecutionJson, String groundTruthExecutionJson, List<Map<String, Object>> differences) {
        this.structurallyEquivalent = structurallyEquivalent;
        this.executionEquivalent = executionEquivalent;
        this.totalCases = 1;
        this.correctCases = executionEquivalent ? 1 : 0;
        this.accuracy = executionEquivalent ? 1.0 : 0.0;
        this.aiGeneratedRuleJson = aiGeneratedRuleJson;
        this.generatedExecutionJson = generatedExecutionJson;
        this.groundTruthExecutionJson = groundTruthExecutionJson;
        this.differences = List.copyOf(differences);
    }

    public boolean isStructurallyEquivalent() { return structurallyEquivalent; }
    public boolean isExecutionEquivalent() { return executionEquivalent; }
    public int getTotalCases() { return totalCases; }
    public int getCorrectCases() { return correctCases; }
    public double getAccuracy() { return accuracy; }
    public String getAiGeneratedRuleJson() { return aiGeneratedRuleJson; }
    public String getGeneratedExecutionJson() { return generatedExecutionJson; }
    public String getGroundTruthExecutionJson() { return groundTruthExecutionJson; }
    public List<Map<String, Object>> getDifferences() { return differences; }
}
