package com.assessly.schemas;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class BenchmarkSchemas {
    public record BenchmarkCaseResponse(String id, String name, String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson) {}

    public record BenchmarkRunRequest(
            @NotBlank String controlText,
            @NotBlank String testDataJson,
            @NotBlank String groundTruthRuleJson,
            String expectedResultJson,
            @NotBlank String aiGeneratedRuleJson
    ) {}

    public record BenchmarkRunResponse(
            boolean structurallyEquivalent,
            boolean executionEquivalent,
            int totalCases,
            int correctCases,
            double accuracy,
            String generatedExecutionJson,
            String groundTruthExecutionJson,
            List<Map<String, Object>> differences
    ) {}
}
