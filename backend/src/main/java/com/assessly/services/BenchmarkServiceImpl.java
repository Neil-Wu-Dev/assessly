package com.assessly.services;

import com.assessly.exceptions.NotFoundException;
import com.assessly.models.BenchmarkCase;
import com.assessly.models.BenchmarkResult;
import com.assessly.models.BenchmarkRun;
import com.assessly.repositories.interfaces.BenchmarkRepository;
import com.assessly.services.interfaces.ApiKeySessionService;
import com.assessly.services.interfaces.BenchmarkService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BenchmarkServiceImpl implements BenchmarkService {
    private final RuleEngine ruleEngine;
    private final ApiKeySessionService apiKeys;
    private final BenchmarkRepository benchmarks;

    public BenchmarkServiceImpl(RuleEngine ruleEngine, ApiKeySessionService apiKeys, BenchmarkRepository benchmarks) {
        this.ruleEngine = ruleEngine;
        this.apiKeys = apiKeys;
        this.benchmarks = benchmarks;
    }

    @Override
    public List<BenchmarkCase> cases(UUID userId) {
        return benchmarks.findCasesVisibleTo(userId);
    }

    @Override
    @Transactional
    public BenchmarkCase createCase(UUID userId, String name, String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson) {
        validateCase(controlText, testDataJson, groundTruthRuleJson, expectedResultJson);
        return benchmarks.saveCase(new BenchmarkCase(userId, name, controlText, testDataJson, groundTruthRuleJson, expectedResultJson, false));
    }

    @Override
    public BenchmarkCase getCase(UUID userId, UUID caseId) {
        return benchmarks.findCaseVisibleTo(caseId, userId).orElseThrow(() -> new NotFoundException("Benchmark case not found."));
    }

    @Override
    @Transactional
    public void deleteCase(UUID userId, UUID caseId) {
        BenchmarkCase benchmarkCase = benchmarks.findOwnedCase(caseId, userId).orElseThrow(() -> new NotFoundException("Custom benchmark case not found."));
        benchmarks.deleteCase(benchmarkCase);
    }

    @Override
    @Transactional
    public BenchmarkResult run(UUID userId, UUID benchmarkCaseId, String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson, String aiGeneratedRuleJson) {
        BenchmarkCase benchmarkCase = null;
        if (benchmarkCaseId != null) {
            benchmarkCase = getCase(userId, benchmarkCaseId);
            controlText = benchmarkCase.getControlText();
            testDataJson = benchmarkCase.getTestDataJson();
            groundTruthRuleJson = benchmarkCase.getGroundTruthRuleJson();
            expectedResultJson = benchmarkCase.getExpectedResultJson();
        }
        BenchmarkResult result = execute(controlText, testDataJson, groundTruthRuleJson, expectedResultJson, aiGeneratedRuleJson, userId);
        benchmarks.saveRun(new BenchmarkRun(
                userId,
                benchmarkCase == null ? null : benchmarkCase.getId(),
                controlText,
                testDataJson,
                groundTruthRuleJson,
                expectedResultJson,
                result
        ));
        return result;
    }

    @Override
    public List<BenchmarkRun> runs(UUID userId) {
        return benchmarks.findRuns(userId);
    }

    @Override
    public BenchmarkRun getRun(UUID userId, UUID runId) {
        return benchmarks.findRun(runId, userId).orElseThrow(() -> new NotFoundException("Benchmark run not found."));
    }

    private void validateCase(String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson) {
        if (controlText == null || controlText.isBlank()) throw new IllegalArgumentException("Control text is required.");
        List<Map<String, Object>> rows = parseRows(testDataJson);
        if (rows.isEmpty()) throw new IllegalArgumentException("Benchmark test data must include at least one structured row.");
        Set<String> fields = rows.get(0).keySet();
        ruleEngine.validateRuleSet(groundTruthRuleJson);
        ruleEngine.validateFields(groundTruthRuleJson, fields);
        if (expectedResultJson != null && !expectedResultJson.isBlank()) JsonSupport.readTree(expectedResultJson);
    }

    private BenchmarkResult execute(String controlText, String testDataJson, String groundTruthRuleJson, String expectedResultJson, String aiGeneratedRuleJson, UUID userId) {
        validateCase(controlText, testDataJson, groundTruthRuleJson, expectedResultJson);
        List<Map<String, Object>> rows = parseRows(testDataJson);
        Set<String> fields = rows.get(0).keySet();
        String generatedRule = aiGeneratedRuleJson == null || aiGeneratedRuleJson.isBlank()
                ? generateRuleWithAi(userId, controlText, fields, testDataJson)
                : aiGeneratedRuleJson;

        ruleEngine.validateRuleSet(generatedRule);
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
