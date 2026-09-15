package com.assessly.mappers;

import com.assessly.models.BenchmarkResult;
import com.assessly.schemas.BenchmarkSchemas.BenchmarkRunResponse;
import org.springframework.stereotype.Component;

@Component
public class BenchmarkMapper {
    public BenchmarkRunResponse toResponse(BenchmarkResult result) {
        return new BenchmarkRunResponse(
                result.isStructurallyEquivalent(),
                result.isExecutionEquivalent(),
                result.getTotalCases(),
                result.getCorrectCases(),
                result.getAccuracy(),
                result.getGeneratedExecutionJson(),
                result.getGroundTruthExecutionJson(),
                result.getDifferences()
        );
    }
}
