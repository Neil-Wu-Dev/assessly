package com.assessly.services.interfaces;

import com.assessly.models.BenchmarkResult;

public interface BenchmarkService {
    BenchmarkResult run(String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson, String aiGeneratedRuleJson);
}
