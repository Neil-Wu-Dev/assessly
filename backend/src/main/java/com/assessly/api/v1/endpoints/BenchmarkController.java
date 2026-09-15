package com.assessly.api.v1.endpoints;

import com.assessly.mappers.BenchmarkMapper;
import com.assessly.schemas.BenchmarkSchemas.*;
import com.assessly.services.interfaces.AuthService;
import com.assessly.services.interfaces.BenchmarkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/benchmarks")
public class BenchmarkController {
    private final AuthService auth;
    private final BenchmarkService benchmarks;
    private final BenchmarkMapper mapper;

    public BenchmarkController(AuthService auth, BenchmarkService benchmarks, BenchmarkMapper mapper) {
        this.auth = auth;
        this.benchmarks = benchmarks;
        this.mapper = mapper;
    }

    @GetMapping("/cases")
    List<BenchmarkCaseResponse> cases(@RequestHeader("X-Assessly-Session") String token) {
        var user = auth.requireUser(token);
        return benchmarks.cases(user.getId()).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/cases")
    BenchmarkCaseResponse createCase(@RequestHeader("X-Assessly-Session") String token, @Valid @RequestBody BenchmarkCaseRequest request) {
        var user = auth.requireUser(token);
        return mapper.toResponse(benchmarks.createCase(user.getId(), request.name(), request.controlText(), request.testDataJson(), request.groundTruthRuleJson(), request.expectedResultJson()));
    }

    @GetMapping("/cases/{caseId}")
    BenchmarkCaseResponse getCase(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID caseId) {
        var user = auth.requireUser(token);
        return mapper.toResponse(benchmarks.getCase(user.getId(), caseId));
    }

    @DeleteMapping("/cases/{caseId}")
    void deleteCase(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID caseId) {
        var user = auth.requireUser(token);
        benchmarks.deleteCase(user.getId(), caseId);
    }

    @PostMapping("/run")
    BenchmarkRunResponse run(@RequestHeader("X-Assessly-Session") String token, @RequestBody BenchmarkRunRequest request) {
        var user = auth.requireUser(token);
        return mapper.toResponse(benchmarks.run(user.getId(), request.benchmarkCaseId(), request.controlText(), request.testDataJson(), request.groundTruthRuleJson(), request.expectedResultJson(), request.aiGeneratedRuleJson()));
    }

    @GetMapping("/runs")
    List<BenchmarkRunRecordResponse> runs(@RequestHeader("X-Assessly-Session") String token) {
        var user = auth.requireUser(token);
        return benchmarks.runs(user.getId()).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/runs/{runId}")
    BenchmarkRunRecordResponse getRun(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID runId) {
        var user = auth.requireUser(token);
        return mapper.toResponse(benchmarks.getRun(user.getId(), runId));
    }
}
