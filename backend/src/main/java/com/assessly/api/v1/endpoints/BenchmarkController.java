package com.assessly.api.v1.endpoints;

import com.assessly.mappers.BenchmarkMapper;
import com.assessly.schemas.BenchmarkSchemas.*;
import com.assessly.services.interfaces.AuthService;
import com.assessly.services.interfaces.BenchmarkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    List<BenchmarkCaseResponse> builtInCases(@RequestHeader("X-Assessly-Session") String token) {
        auth.requireUser(token);
        return List.of(new BenchmarkCaseResponse(
                "builtin-iam-mfa-001",
                "Administrative MFA",
                "Administrative accounts must use MFA.",
                "[{\"role\":\"admin\",\"mfa_enabled\":false},{\"role\":\"user\",\"mfa_enabled\":true}]",
                "{\"rules\":[{\"id\":\"gt-iam-mfa\",\"sourceControl\":{\"controlId\":\"IAM-04\",\"text\":\"Administrative accounts must use MFA.\"},\"ast\":{\"type\":\"if\",\"if\":{\"type\":\"condition\",\"field\":\"role\",\"operator\":\"=\",\"value\":\"admin\"},\"then\":{\"type\":\"condition\",\"field\":\"mfa_enabled\",\"operator\":\"=\",\"value\":true}}}]}",
                ""
        ));
    }

    @PostMapping("/run")
    BenchmarkRunResponse run(@RequestHeader("X-Assessly-Session") String token, @Valid @RequestBody BenchmarkRunRequest request) {
        auth.requireUser(token);
        return mapper.toResponse(benchmarks.run(request.controlText(), request.testDataJson(), request.groundTruthRuleJson(), request.expectedResultJson(), request.aiGeneratedRuleJson()));
    }
}
