package com.assessly.mappers;

import com.assessly.models.BenchmarkCase;
import com.assessly.models.BenchmarkResult;
import com.assessly.models.BenchmarkRun;
import com.assessly.schemas.BenchmarkSchemas.BenchmarkCaseResponse;
import com.assessly.schemas.BenchmarkSchemas.BenchmarkRunRecordResponse;
import com.assessly.schemas.BenchmarkSchemas.BenchmarkRunResponse;
import org.springframework.stereotype.Component;

@Component
public class BenchmarkMapper {
    public BenchmarkCaseResponse toResponse(BenchmarkCase benchmarkCase) {
        return new BenchmarkCaseResponse(
                benchmarkCase.getId().toString(),
                benchmarkCase.getName(),
                benchmarkCase.getControlText(),
                benchmarkCase.getTestDataJson(),
                benchmarkCase.getGroundTruthRuleJson(),
                benchmarkCase.getExpectedResultJson(),
                benchmarkCase.isBuiltin(),
                benchmarkCase.getCreatedAt()
        );
    }

    public BenchmarkRunResponse toResponse(BenchmarkResult result) {
        return new BenchmarkRunResponse(
                result.isStructurallyEquivalent(),
                result.isExecutionEquivalent(),
                result.getTotalCases(),
                result.getCorrectCases(),
                result.getAccuracy(),
                result.getAiGeneratedRuleJson(),
                result.getGeneratedExecutionJson(),
                result.getGroundTruthExecutionJson(),
                result.getDifferences()
        );
    }

    public BenchmarkRunRecordResponse toResponse(BenchmarkRun run) {
        return new BenchmarkRunRecordResponse(
                run.getId().toString(),
                run.getBenchmarkCaseId() == null ? null : run.getBenchmarkCaseId().toString(),
                run.getControlText(),
                run.getTestDataJson(),
                run.getGroundTruthRuleJson(),
                run.getExpectedResultJson(),
                run.isStructurallyEquivalent(),
                run.isExecutionEquivalent(),
                run.getAccuracy(),
                run.getAiGeneratedRuleJson(),
                run.getGeneratedExecutionJson(),
                run.getGroundTruthExecutionJson(),
                run.getDifferencesJson(),
                run.getCreatedAt()
        );
    }
}
