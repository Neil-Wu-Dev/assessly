package com.assessly.services;

import com.assessly.defs.RuleGenerationStatus;
import com.assessly.exceptions.AuthenticationException;
import com.assessly.exceptions.NotFoundException;
import com.assessly.exceptions.RuleValidationException;
import com.assessly.models.ControlDocument;
import com.assessly.models.EvidenceFile;
import com.assessly.models.RuleSet;
import com.assessly.repositories.interfaces.ControlRepository;
import com.assessly.repositories.interfaces.EvidenceRepository;
import com.assessly.repositories.interfaces.RuleSetRepository;
import com.assessly.services.interfaces.ApiKeySessionService;
import com.assessly.services.interfaces.DatasetService;
import com.assessly.services.interfaces.RuleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RuleServiceImpl implements RuleService {
    private final DatasetService datasets;
    private final RuleSetRepository ruleSets;
    private final EvidenceRepository evidence;
    private final ControlRepository controls;
    private final ApiKeySessionService apiKeys;
    private final RuleEngine ruleEngine;

    public RuleServiceImpl(DatasetService datasets, RuleSetRepository ruleSets, EvidenceRepository evidence, ControlRepository controls, ApiKeySessionService apiKeys, RuleEngine ruleEngine) {
        this.datasets = datasets;
        this.ruleSets = ruleSets;
        this.evidence = evidence;
        this.controls = controls;
        this.apiKeys = apiKeys;
        this.ruleEngine = ruleEngine;
    }

    public RuleSet saveManual(UUID ownerId, UUID datasetId, String rulesJson, String summaryJson) {
        datasets.get(ownerId, datasetId);
        EvidenceFile latestEvidence = latestEvidence(datasetId);
        Set<String> availableFields = fieldsFromColumns(latestEvidence.getColumnsJson());
        ruleEngine.validateRuleSet(rulesJson);
        ruleEngine.validateFields(rulesJson, availableFields);
        RuleSet ruleSet = new RuleSet(datasetId, ruleSets.nextVersion(datasetId), RuleGenerationStatus.READY, rulesJson, blankSummary(summaryJson, "Manual Visual Rule Builder"));
        return ruleSets.save(ruleSet);
    }

    public RuleSet generate(UUID ownerId, UUID datasetId) {
        datasets.get(ownerId, datasetId);
        EvidenceFile latestEvidence;
        List<ControlDocument> docs;
        try {
            latestEvidence = latestEvidence(datasetId);
            docs = controls.findDocumentsByDataset(datasetId);
            if (docs.isEmpty()) return cannot(datasetId, "Security Control documents are missing.");
            apiKeys.requireApiKey(ownerId);
        } catch (AuthenticationException e) {
            return error(datasetId, e.getMessage());
        } catch (RuleValidationException e) {
            return cannot(datasetId, e.getMessage());
        }

        Set<String> availableFields = fieldsFromColumns(latestEvidence.getColumnsJson());
        String content;
        try {
            content = apiKeys.chatCompletion(ownerId, List.of(
                    Map.of("role", "system", "content", systemPrompt()),
                    Map.of("role", "user", "content", userPrompt(docs, latestEvidence, availableFields))
            ));
        } catch (RuntimeException e) {
            return error(datasetId, e.getMessage());
        }

        try {
            JsonNode output = JsonSupport.readTree(extractJson(content));
            String status = output.path("status").asText("ERROR");
            if ("CANNOT_AUTOMATE".equals(status)) {
                return cannot(datasetId, output.path("reason").asText("AI reported that the controls cannot be automated against the current evidence schema."));
            }
            if (!"READY".equals(status)) {
                return error(datasetId, output.path("reason").asText("Model response did not return READY or CANNOT_AUTOMATE."));
            }
            ObjectNode rulesRoot = JsonSupport.MAPPER.createObjectNode();
            rulesRoot.set("rules", output.get("rules"));
            String rulesJson = JsonSupport.write(rulesRoot);
            ruleEngine.validateRuleSet(rulesJson);
            ruleEngine.validateFields(rulesJson, availableFields);
            String summary = JsonSupport.write(Map.of(
                    "status", "READY",
                    "source", "AI Provider",
                    "evidenceFile", latestEvidence.getFilename(),
                    "availableFields", availableFields,
                    "modelReasoning", output.path("summary").asText("Rules generated from uploaded Security Controls and current evidence schema.")
            ));
            return ruleSets.save(new RuleSet(datasetId, ruleSets.nextVersion(datasetId), RuleGenerationStatus.READY, rulesJson, summary));
        } catch (RuleValidationException e) {
            return error(datasetId, e.getMessage());
        } catch (RuntimeException e) {
            return error(datasetId, "Model response format error or JSON/AST validation failed: " + e.getMessage());
        }
    }

    public RuleSet get(UUID ownerId, UUID datasetId, UUID ruleSetId) {
        datasets.get(ownerId, datasetId);
        return ruleSets.findByIdAndDataset(ruleSetId, datasetId).orElseThrow(() -> new NotFoundException("Rule Set not found."));
    }

    public RuleSet update(UUID ownerId, UUID datasetId, UUID ruleSetId, String rulesJson, String summaryJson) {
        RuleSet existing = get(ownerId, datasetId, ruleSetId);
        if (existing.getConfirmedAt() != null) {
            throw new RuleValidationException("Confirmed Rule Sets are immutable. Save a new manual Rule Set instead.");
        }
        return saveManual(ownerId, datasetId, rulesJson, summaryJson);
    }

    public RuleSet confirm(UUID ownerId, UUID datasetId, UUID ruleSetId) {
        datasets.get(ownerId, datasetId);
        EvidenceFile latestEvidence = latestEvidence(datasetId);
        RuleSet ruleSet = get(ownerId, datasetId, ruleSetId);
        ruleEngine.validateRuleSet(ruleSet.getRulesJson());
        ruleEngine.validateFields(ruleSet.getRulesJson(), fieldsFromColumns(latestEvidence.getColumnsJson()));
        ruleSet.confirmExecutable();
        return ruleSets.save(ruleSet);
    }

    public List<RuleSet> list(UUID ownerId, UUID datasetId) {
        datasets.get(ownerId, datasetId);
        return ruleSets.findAllByDataset(datasetId);
    }

    private EvidenceFile latestEvidence(UUID datasetId) {
        return evidence.findByDataset(datasetId).stream().findFirst().orElseThrow(() -> new RuleValidationException("Current Evidence is missing."));
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

    private String systemPrompt() {
        return "You translate cybersecurity Security Controls into Assessly's constrained JSON rule language. " +
                "Never generate code. Never invent thresholds or values not present in the control text. " +
                "Return only one JSON object. Use status READY when deterministic machine rules can be generated. " +
                "Use CANNOT_AUTOMATE when the control lacks measurable criteria, needed fields are absent, or the rule language cannot express it. " +
                "Allowed AST node types: condition, and, or, not, if. A condition requires field, operator, value. " +
                "Allowed operators: =, !=, >, >=, <, <=, IN, NOT IN, CONTAINS. " +
                "READY response shape: {\"status\":\"READY\",\"summary\":\"...\",\"rules\":[{\"id\":\"...\",\"fieldsUsed\":[\"...\"],\"sourceControl\":{\"controlId\":\"...\",\"text\":\"...\"},\"explanation\":\"...\",\"ast\":{...}}]}. " +
                "CANNOT_AUTOMATE response shape: {\"status\":\"CANNOT_AUTOMATE\",\"reason\":\"...\",\"rules\":[]}.";
    }

    private String userPrompt(List<ControlDocument> docs, EvidenceFile latestEvidence, Set<String> availableFields) {
        StringBuilder controlsText = new StringBuilder();
        for (ControlDocument doc : docs) {
            controlsText.append("Document: ").append(doc.getFilename()).append("\n");
            controlsText.append(doc.getFullText()).append("\n\n");
        }
        return "Evidence file: " + latestEvidence.getFilename() + "\n" +
                "Available evidence fields: " + availableFields + "\n" +
                "Evidence columns JSON: " + latestEvidence.getColumnsJson() + "\n" +
                "Security Controls:\n" + controlsText;
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int first = trimmed.indexOf('{');
            int last = trimmed.lastIndexOf('}');
            if (first >= 0 && last > first) return trimmed.substring(first, last + 1);
        }
        return trimmed;
    }

    private RuleSet cannot(UUID datasetId, String reason) {
        return ruleSets.save(new RuleSet(datasetId, ruleSets.nextVersion(datasetId), RuleGenerationStatus.CANNOT_AUTOMATE, "{\"rules\":[]}", JsonSupport.write(Map.of("reason", reason))));
    }

    private RuleSet error(UUID datasetId, String reason) {
        return ruleSets.save(new RuleSet(datasetId, ruleSets.nextVersion(datasetId), RuleGenerationStatus.ERROR, "{\"rules\":[]}", JsonSupport.write(Map.of("reason", reason))));
    }

    private String blankSummary(String summaryJson, String source) {
        return summaryJson == null || summaryJson.isBlank() ? JsonSupport.write(Map.of("source", source)) : summaryJson;
    }
}
