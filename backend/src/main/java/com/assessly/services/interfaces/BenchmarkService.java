package com.assessly.services.interfaces;

import com.assessly.models.BenchmarkCase;
import com.assessly.models.BenchmarkResult;
import com.assessly.models.BenchmarkRun;

import java.util.List;
import java.util.UUID;

public interface BenchmarkService {
    List<BenchmarkCase> cases(UUID userId);
    BenchmarkCase createCase(UUID userId, String name, String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson);
    BenchmarkCase getCase(UUID userId, UUID caseId);
    void deleteCase(UUID userId, UUID caseId);
    BenchmarkResult run(UUID userId, UUID benchmarkCaseId, String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson, String aiGeneratedRuleJson);
    List<BenchmarkRun> runs(UUID userId);
    BenchmarkRun getRun(UUID userId, UUID runId);
}
