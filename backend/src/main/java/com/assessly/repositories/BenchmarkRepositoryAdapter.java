package com.assessly.repositories;

import com.assessly.models.BenchmarkCase;
import com.assessly.models.BenchmarkRun;
import com.assessly.repositories.interfaces.BenchmarkRepository;
import com.assessly.repositories.jpa.BenchmarkCaseJpaRepository;
import com.assessly.repositories.jpa.BenchmarkRunJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BenchmarkRepositoryAdapter implements BenchmarkRepository {
    private final BenchmarkCaseJpaRepository cases;
    private final BenchmarkRunJpaRepository runs;

    public BenchmarkRepositoryAdapter(BenchmarkCaseJpaRepository cases, BenchmarkRunJpaRepository runs) {
        this.cases = cases;
        this.runs = runs;
    }

    @Override
    public List<BenchmarkCase> findCasesVisibleTo(UUID ownerId) { return cases.findVisibleCases(ownerId); }

    @Override
    public Optional<BenchmarkCase> findCaseVisibleTo(UUID caseId, UUID ownerId) { return cases.findVisibleById(caseId, ownerId); }

    @Override
    public Optional<BenchmarkCase> findOwnedCase(UUID caseId, UUID ownerId) { return cases.findByIdAndOwnerId(caseId, ownerId); }

    @Override
    public BenchmarkCase saveCase(BenchmarkCase benchmarkCase) { return cases.save(benchmarkCase); }

    @Override
    public void deleteCase(BenchmarkCase benchmarkCase) { cases.delete(benchmarkCase); }

    @Override
    public BenchmarkRun saveRun(BenchmarkRun run) { return runs.save(run); }

    @Override
    public List<BenchmarkRun> findRuns(UUID ownerId) { return runs.findByOwnerIdOrderByCreatedAtDesc(ownerId); }

    @Override
    public Optional<BenchmarkRun> findRun(UUID runId, UUID ownerId) { return runs.findByIdAndOwnerId(runId, ownerId); }
}
