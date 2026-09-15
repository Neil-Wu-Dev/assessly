package com.assessly.services;

import com.assessly.defs.AssessmentStatus;
import com.assessly.exceptions.NotFoundException;
import com.assessly.exceptions.RuleValidationException;
import com.assessly.models.AssessmentRun;
import com.assessly.models.EvidenceFile;
import com.assessly.models.RuleSet;
import com.assessly.repositories.interfaces.AssessmentRepository;
import com.assessly.repositories.interfaces.RuleSetRepository;
import com.assessly.services.interfaces.AssessmentService;
import com.assessly.services.interfaces.DatasetService;
import com.assessly.services.interfaces.EvidenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AssessmentServiceImpl implements AssessmentService {
    private final DatasetService datasets;
    private final EvidenceService evidence;
    private final RuleSetRepository ruleSets;
    private final AssessmentRepository assessments;
    private final RuleEngine ruleEngine;

    public AssessmentServiceImpl(DatasetService datasets, EvidenceService evidence, RuleSetRepository ruleSets, AssessmentRepository assessments, RuleEngine ruleEngine) {
        this.datasets = datasets;
        this.evidence = evidence;
        this.ruleSets = ruleSets;
        this.assessments = assessments;
        this.ruleEngine = ruleEngine;
    }

    public AssessmentRun start(UUID ownerId, UUID datasetId, UUID evidenceFileId, UUID ruleSetId) {
        datasets.get(ownerId, datasetId);
        EvidenceFile evidenceFile = evidence.get(ownerId, datasetId, evidenceFileId);
        RuleSet ruleSet = ruleSets.findByIdAndDataset(ruleSetId, datasetId).orElseThrow(() -> new NotFoundException("Rule Set not found."));
        if (!ruleSet.isExecutable()) throw new RuleValidationException("Rule Set is not confirmed or executable.");
        ruleEngine.validateRuleSet(ruleSet.getRulesJson());
        ruleEngine.validateFields(ruleSet.getRulesJson(), fieldsFromColumns(evidenceFile.getColumnsJson()));
        List<Map<String, Object>> rows = JsonSupport.read(evidenceFile.getRowsJson(), new TypeReference<>() {});
        Map<String, Object> result = ruleEngine.evaluate(ruleSet.getRulesJson(), rows);
        return assessments.save(new AssessmentRun(datasetId, ruleSet.getId(), evidenceFile.getId(), AssessmentStatus.COMPLETED, (int) result.get("recordsEvaluated"), (int) result.get("rulesEvaluated"), (int) result.get("violationsDetected"), JsonSupport.write(result)));
    }

    public List<AssessmentRun> history(UUID ownerId, UUID datasetId) {
        datasets.get(ownerId, datasetId);
        return assessments.findByDataset(datasetId);
    }

    public AssessmentRun get(UUID ownerId, UUID datasetId, UUID assessmentId) {
        datasets.get(ownerId, datasetId);
        return assessments.findByIdAndDataset(assessmentId, datasetId).orElseThrow(() -> new NotFoundException("Assessment run not found."));
    }

    private Set<String> fieldsFromColumns(String columnsJson) {
        List<Map<String, Object>> columns = JsonSupport.read(columnsJson, new TypeReference<>() {});
        Set<String> fields = new LinkedHashSet<>();
        for (Map<String, Object> column : columns) {
            Object name = column.get("name");
            if (name != null) fields.add(String.valueOf(name));
        }
        return fields;
    }
}
