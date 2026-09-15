package com.assessly.repositories.interfaces;

import com.assessly.models.BenchmarkCase;
import com.assessly.models.BenchmarkRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BenchmarkRepository {
    List<BenchmarkCase> findCasesVisibleTo(UUID ownerId);
    Optional<BenchmarkCase> findCaseVisibleTo(UUID caseId, UUID ownerId);
    Optional<BenchmarkCase> findOwnedCase(UUID caseId, UUID ownerId);
    BenchmarkCase saveCase(BenchmarkCase benchmarkCase);
    void deleteCase(BenchmarkCase benchmarkCase);
    BenchmarkRun saveRun(BenchmarkRun run);
    List<BenchmarkRun> findRuns(UUID ownerId);
    Optional<BenchmarkRun> findRun(UUID runId, UUID ownerId);
}
