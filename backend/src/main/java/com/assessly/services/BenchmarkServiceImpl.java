package com.assessly.services;

import com.assessly.models.BenchmarkResult;
import com.assessly.services.interfaces.ApiKeySessionService;
import com.assessly.services.interfaces.BenchmarkService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.UUID;

@Service
public class BenchmarkServiceImpl implements BenchmarkService {
    private final RuleEngine ruleEngine;
    private final ApiKeySessionService apiKeys;

    public BenchmarkServiceImpl(RuleEngine ruleEngine, ApiKeySessionService apiKeys) {
        this.ruleEngine = ruleEngine;
        this.apiKeys = apiKeys;
    }

    @Override
    public BenchmarkResult run(UUID userId, String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson, String aiGeneratedRuleJson) {
        List<Map<String, Object>> rows = parseRows(testDataJson);
        Set<String> fields = rows.isEmpty() ? Set.of() : rows.get(0).keySet();
        String generatedRule = aiGeneratedRuleJson == null || aiGeneratedRuleJson.isBlank()
                ? generateRuleWithAi(userId, controlText, fields, testDataJson)
                : aiGeneratedRuleJson;

        ruleEngine.validateRuleSet(groundTruthRuleJson);
        ruleEngine.validateRuleSet(generatedRule);
        ruleEngine.validateFields(groundTruthRuleJson, fields);
        ruleEngine.validateFields(generatedRule, fields);

        Map<String, Object> generatedExecution = ruleEngine.evaluate(generatedRule, rows);
        Map<String, Object> groundTruthExecution = expectedResultJson == null || expectedResultJson.isBlank()
                ? ruleEngine.evaluate(groundTruthRuleJson, rows)
                : JsonSupport.read(expectedResultJson, new TypeReference<>() {});

        boolean structural = canonicalRules(generatedRule).equals(canonicalRules(groundTruthRuleJson));
        boolean execution = executionSignature(generatedExecution).equals(executionSignature(groundTruthExecution));
        List<Map<String, Object>> differences = new ArrayList<>();
        if (!structural) differences.add(Map.of("type", "AST_STRUCTURAL_DIFFERENCE", "message", "Generated rule AST is not structurally equivalent to the human ground-truth rule."));
        if (!execution) differences.add(Map.of("type", "EXECUTION_RESULT_DIFFERENCE", "message", "Generated rule execution result differs from the expected assessment result."));
        if (controlText == null || controlText.isBlank()) differences.add(Map.of("type", "MISSING_CONTROL_TEXT", "message", "Control text is required for traceability."));

        return new BenchmarkResult(structural, execution, generatedRule, JsonSupport.write(generatedExecution), JsonSupport.write(groundTruthExecution), differences);
    }

    private String generateRuleWithAi(UUID userId, String controlText, Set<String> fields, String testDataJson) {
        String content = apiKeys.chatCompletion(userId, List.of(
                Map.of("role", "system", "content", "Translate one Security Control into Assessly JSON rule language. Return only JSON with shape {\"rules\":[{\"id\":\"...\",\"fieldsUsed\":[\"...\"],\"sourceControl\":{\"controlId\":\"...\",\"text\":\"...\"},\"explanation\":\"...\",\"ast\":{...}}]}. Allowed AST node types: condition, and, or, not, if. Allowed operators: =, !=, >, >=, <, <=, IN, NOT IN, CONTAINS. Do not invent thresholds or fields."),
                Map.of("role", "user", "content", "Control text:\n" + controlText + "\nAvailable fields: " + fields + "\nTest data:\n" + testDataJson)
        ));
        String json = extractJson(content);
        JsonNode root = JsonSupport.readTree(json);
        if (root.has("rules")) return JsonSupport.write(root);
        if (root.has("status") && root.has("rules")) {
            ObjectNode normalized = JsonSupport.MAPPER.createObjectNode();
            normalized.set("rules", root.get("rules"));
            return JsonSupport.write(normalized);
        }
        throw new IllegalArgumentException("AI benchmark response did not include a rules array.");
    }

    private List<Map<String, Object>> parseRows(String testDataJson) {
        JsonNode root = JsonSupport.readTree(testDataJson);
        JsonNode rowsNode = root.isArray() ? root : root.get("rows");
        if (rowsNode == null || !rowsNode.isArray()) return List.of();
        return JsonSupport.MAPPER.convertValue(rowsNode, new TypeReference<>() {});
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

    private String canonicalRules(String rulesJson) {
        JsonNode root = JsonSupport.readTree(rulesJson);
        JsonNode rules = root.has("rules") ? root.get("rules") : root;
        ArrayNode canonical = JsonSupport.MAPPER.createArrayNode();
        List<JsonNode> normalized = new ArrayList<>();
        rules.forEach(rule -> normalized.add(normalizeRule(rule)));
        normalized.stream().sorted(Comparator.comparing(JsonNode::toString)).forEach(canonical::add);
        return canonical.toString();
    }

    private JsonNode normalizeRule(JsonNode rule) {
        ObjectNode normalized = JsonSupport.MAPPER.createObjectNode();
        normalized.set("ast", normalizeAst(rule.get("ast")));
        normalized.set("sourceControl", rule.get("sourceControl"));
        return normalized;
    }

    private JsonNode normalizeAst(JsonNode node) {
        String type = node.get("type").asText();
        ObjectNode normalized = JsonSupport.MAPPER.createObjectNode();
        normalized.put("type", type);
        switch (type) {
            case "and", "or" -> {
                ArrayNode children = JsonSupport.MAPPER.createArrayNode();
                List<JsonNode> normalizedChildren = new ArrayList<>();
                node.get("children").forEach(child -> normalizedChildren.add(normalizeAst(child)));
                normalizedChildren.stream().sorted(Comparator.comparing(JsonNode::toString)).forEach(children::add);
                normalized.set("children", children);
            }
            case "not" -> normalized.set("child", normalizeAst(node.get("child")));
            case "if" -> {
                normalized.set("if", normalizeAst(node.get("if")));
                normalized.set("then", normalizeAst(node.get("then")));
                if (node.has("else")) normalized.set("else", normalizeAst(node.get("else")));
            }
            default -> {
                normalized.set("field", node.get("field"));
                normalized.set("operator", node.get("operator"));
                normalized.set("value", node.get("value"));
            }
        }
        return normalized;
    }

    private Map<String, Object> executionSignature(Map<String, Object> execution) {
        Object violations = execution.get("violations");
        List<Integer> indexes = new ArrayList<>();
        if (violations instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && map.get("recordIndex") != null) indexes.add(Integer.parseInt(String.valueOf(map.get("recordIndex"))));
            }
        }
        Collections.sort(indexes);
        return Map.of(
                "recordsEvaluated", execution.getOrDefault("recordsEvaluated", 0),
                "rulesEvaluated", execution.getOrDefault("rulesEvaluated", 0),
                "violationsDetected", execution.getOrDefault("violationsDetected", 0),
                "violationRecordIndexes", indexes
        );
    }
}
