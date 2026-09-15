package com.assessly.services.interfaces;

import com.assessly.models.BenchmarkResult;

import java.util.UUID;

public interface BenchmarkService {
    BenchmarkResult run(UUID userId, String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson, String aiGeneratedRuleJson);
}
