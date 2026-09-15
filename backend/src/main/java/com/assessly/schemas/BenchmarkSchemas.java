package com.assessly.schemas;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BenchmarkSchemas {
    public record BenchmarkCaseRequest(
            @NotBlank String name,
            @NotBlank String controlText,
            @NotBlank String testDataJson,
            @NotBlank String groundTruthRuleJson,
            String expectedResultJson
    ) {}

    public record BenchmarkCaseResponse(
            String id,
            String name,
            String controlText,
            String testDataJson,
            String groundTruthRuleJson,
            String expectedResultJson,
            boolean builtin,
            Instant createdAt
    ) {}

    public record BenchmarkRunRequest(
            UUID benchmarkCaseId,
            String controlText,
            String testDataJson,
            String groundTruthRuleJson,
            String expectedResultJson,
            String aiGeneratedRuleJson
    ) {}

    public record BenchmarkRunResponse(
            boolean structurallyEquivalent,
            boolean executionEquivalent,
            int totalCases,
            int correctCases,
            double accuracy,
            String aiGeneratedRuleJson,
            String generatedExecutionJson,
            String groundTruthExecutionJson,
            List<Map<String, Object>> differences
    ) {}

    public record BenchmarkRunRecordResponse(
            String id,
            String benchmarkCaseId,
            String controlText,
            String testDataJson,
            String groundTruthRuleJson,
            String expectedResultJson,
            boolean structurallyEquivalent,
            boolean executionEquivalent,
            double accuracy,
            String aiGeneratedRuleJson,
            String generatedExecutionJson,
            String groundTruthExecutionJson,
            String differencesJson,
            Instant createdAt
    ) {}
}
