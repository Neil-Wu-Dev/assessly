package com.assessly.api.v1.endpoints;

import com.assessly.mappers.*;
import com.assessly.schemas.AssessmentSchemas.*;
import com.assessly.schemas.DatasetSchemas.*;
import com.assessly.schemas.RuleSchemas.*;
import com.assessly.services.interfaces.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/datasets")
public class DatasetController {
    private final AuthService auth;
    private final DatasetService datasets;
    private final EvidenceService evidence;
    private final ControlService controls;
    private final RuleService rules;
    private final AssessmentService assessments;
    private final DatasetMapper datasetMapper;
    private final RuleMapper ruleMapper;
    private final AssessmentMapper assessmentMapper;

    public DatasetController(AuthService auth, DatasetService datasets, EvidenceService evidence, ControlService controls, RuleService rules, AssessmentService assessments, DatasetMapper datasetMapper, RuleMapper ruleMapper, AssessmentMapper assessmentMapper) {
        this.auth = auth;
        this.datasets = datasets;
        this.evidence = evidence;
        this.controls = controls;
        this.rules = rules;
        this.assessments = assessments;
        this.datasetMapper = datasetMapper;
        this.ruleMapper = ruleMapper;
        this.assessmentMapper = assessmentMapper;
    }

    @PostMapping
    DatasetResponse create(@RequestHeader("X-Assessly-Session") String token, @Valid @RequestBody DatasetRequest request) {
        return datasetMapper.toResponse(datasets.create(auth.requireUser(token).getId(), request.name(), request.description()));
    }

    @GetMapping
    Object list(@RequestHeader("X-Assessly-Session") String token) {
        return datasets.list(auth.requireUser(token).getId()).stream().map(datasetMapper::toResponse).toList();
    }

    @GetMapping("/{datasetId}")
    DatasetDetail detail(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId) {
        UUID ownerId = auth.requireUser(token).getId();
        return new DatasetDetail(
                datasetMapper.toResponse(datasets.get(ownerId, datasetId)),
                evidence.list(ownerId, datasetId).stream().map(datasetMapper::toResponse).toList(),
                controls.list(ownerId, datasetId).stream().map(datasetMapper::toResponse).toList(),
                rules.list(ownerId, datasetId).stream().map(ruleMapper::toResponse).toList(),
                assessments.history(ownerId, datasetId).stream().map(assessmentMapper::toResponse).toList()
        );
    }

    @PatchMapping("/{datasetId}")
    DatasetResponse update(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @Valid @RequestBody DatasetRequest request) {
        return datasetMapper.toResponse(datasets.update(auth.requireUser(token).getId(), datasetId, request.name(), request.description()));
    }

    @DeleteMapping("/{datasetId}")
    void delete(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId) {
        datasets.delete(auth.requireUser(token).getId(), datasetId);
    }

    @PostMapping("/{datasetId}/evidence")
    EvidenceResponse uploadEvidence(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @RequestPart MultipartFile file) {
        return datasetMapper.toResponse(evidence.upload(auth.requireUser(token).getId(), datasetId, file));
    }

    @GetMapping("/{datasetId}/evidence")
    Object listEvidence(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId) {
        return evidence.list(auth.requireUser(token).getId(), datasetId).stream().map(datasetMapper::toResponse).toList();
    }

    @GetMapping("/{datasetId}/evidence/{evidenceId}")
    EvidenceResponse getEvidence(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID evidenceId) {
        return datasetMapper.toResponse(evidence.get(auth.requireUser(token).getId(), datasetId, evidenceId));
    }

    @GetMapping("/{datasetId}/evidence/{evidenceId}/rows")
    EvidenceRowsResponse evidenceRows(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID evidenceId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return new EvidenceRowsResponse(evidenceId, page, size, evidence.rows(auth.requireUser(token).getId(), datasetId, evidenceId, page, size));
    }

    @DeleteMapping("/{datasetId}/evidence/{evidenceId}")
    void deleteEvidence(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID evidenceId) {
        evidence.delete(auth.requireUser(token).getId(), datasetId, evidenceId);
    }

    @PostMapping("/{datasetId}/controls")
    ControlDocumentResponse uploadControl(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @RequestPart MultipartFile file) {
        return datasetMapper.toResponse(controls.upload(auth.requireUser(token).getId(), datasetId, file));
    }

    @GetMapping("/{datasetId}/controls")
    Object listControls(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId) {
        return controls.list(auth.requireUser(token).getId(), datasetId).stream().map(datasetMapper::toResponse).toList();
    }

    @GetMapping("/{datasetId}/controls/{controlId}")
    ControlDocumentResponse getControl(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID controlId) {
        return datasetMapper.toResponse(controls.get(auth.requireUser(token).getId(), datasetId, controlId));
    }

    @DeleteMapping("/{datasetId}/controls/{controlId}")
    void deleteControl(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID controlId) {
        controls.delete(auth.requireUser(token).getId(), datasetId, controlId);
    }

    @GetMapping("/{datasetId}/controls/{controlId}/chunks")
    Object controlChunks(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID controlId) {
        return controls.chunks(auth.requireUser(token).getId(), datasetId, controlId).stream().map(datasetMapper::toResponse).toList();
    }

    @PostMapping("/{datasetId}/rules/generate")
    RuleSetResponse generateRules(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId) {
        return ruleMapper.toResponse(rules.generate(auth.requireUser(token).getId(), datasetId));
    }

    @GetMapping("/{datasetId}/rules")
    Object listRules(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId) {
        return rules.list(auth.requireUser(token).getId(), datasetId).stream().map(ruleMapper::toResponse).toList();
    }

    @GetMapping("/{datasetId}/rules/{ruleSetId}")
    RuleSetResponse getRule(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID ruleSetId) {
        return ruleMapper.toResponse(rules.get(auth.requireUser(token).getId(), datasetId, ruleSetId));
    }

    @PostMapping("/{datasetId}/rules/manual")
    RuleSetResponse saveManualRules(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @Valid @RequestBody ManualRuleSetRequest request) {
        return ruleMapper.toResponse(rules.saveManual(auth.requireUser(token).getId(), datasetId, request.rulesJson(), request.generationSummaryJson()));
    }

    @PatchMapping("/{datasetId}/rules/{ruleSetId}")
    RuleSetResponse updateRule(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID ruleSetId, @Valid @RequestBody ManualRuleSetRequest request) {
        return ruleMapper.toResponse(rules.update(auth.requireUser(token).getId(), datasetId, ruleSetId, request.rulesJson(), request.generationSummaryJson()));
    }

    @PostMapping("/{datasetId}/rules/{ruleSetId}/confirm")
    RuleSetResponse confirmRules(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID ruleSetId) {
        return ruleMapper.toResponse(rules.confirm(auth.requireUser(token).getId(), datasetId, ruleSetId));
    }

    @PostMapping("/{datasetId}/assessments")
    AssessmentRunResponse startAssessment(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @RequestBody StartAssessmentRequest request) {
        return assessmentMapper.toResponse(assessments.start(auth.requireUser(token).getId(), datasetId, request.evidenceFileId(), request.ruleSetId()));
    }

    @GetMapping("/{datasetId}/assessments")
    Object listAssessments(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId) {
        return assessments.history(auth.requireUser(token).getId(), datasetId).stream().map(assessmentMapper::toResponse).toList();
    }

    @GetMapping("/{datasetId}/assessments/{assessmentId}")
    AssessmentRunResponse getAssessment(@RequestHeader("X-Assessly-Session") String token, @PathVariable UUID datasetId, @PathVariable UUID assessmentId) {
        return assessmentMapper.toResponse(assessments.get(auth.requireUser(token).getId(), datasetId, assessmentId));
    }
}
