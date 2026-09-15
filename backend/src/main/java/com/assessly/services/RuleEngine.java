package com.assessly.services;

import com.assessly.exceptions.RuleValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RuleEngine {
    public void validateRuleSet(String rulesJson) {
        JsonNode root = JsonSupport.readTree(rulesJson);
        JsonNode rules = root.has("rules") ? root.get("rules") : root;
        if (!rules.isArray() || rules.isEmpty()) throw new RuleValidationException("Rule set must contain a non-empty rules array.");
        rules.forEach(rule -> {
            if (!rule.hasNonNull("id") || !rule.has("ast") || !rule.has("sourceControl")) {
                throw new RuleValidationException("Each rule must include id, ast, and sourceControl.");
            }
            validateNode(rule.get("ast"));
        });
    }

    public void validateFields(String rulesJson, Set<String> availableFields) {
        Set<String> requiredFields = RuleFieldExtractor.fields(rulesJson);
        Set<String> missing = new LinkedHashSet<>(requiredFields);
        missing.removeAll(availableFields);
        if (!missing.isEmpty()) {
            throw new RuleValidationException("Schema mismatch: rule references missing field(s): " + String.join(", ", missing));
        }
    }

    public Map<String, Object> evaluate(String rulesJson, List<Map<String, Object>> rows) {
        JsonNode rules = JsonSupport.readTree(rulesJson).get("rules");
        List<Map<String, Object>> violations = new ArrayList<>();
        int recordIndex = 0;
        for (Map<String, Object> row : rows) {
            for (JsonNode rule : rules) {
                boolean passed = eval(rule.get("ast"), row);
                if (!passed) {
                    violations.add(Map.of(
                            "recordIndex", recordIndex,
                            "actualRecord", row,
                            "fieldsUsed", rule.path("fieldsUsed"),
                            "machineRule", rule,
                            "sourceControl", rule.get("sourceControl"),
                            "expected", "Rule expression evaluates to true"
                    ));
                }
            }
            recordIndex++;
        }
        return Map.of(
                "recordsEvaluated", rows.size(),
                "rulesEvaluated", rules.size(),
                "violationsDetected", violations.size(),
                "violations", violations
        );
    }

    private void validateNode(JsonNode node) {
        String type = requiredText(node, "type");
        switch (type) {
            case "condition" -> {
                requiredText(node, "field");
                requiredText(node, "operator");
                if (!node.has("value")) throw new RuleValidationException("Condition nodes require a value.");
            }
            case "and", "or" -> {
                JsonNode children = node.get("children");
                if (children == null || !children.isArray() || children.size() < 2) throw new RuleValidationException(type + " nodes require at least two children.");
                children.forEach(this::validateNode);
            }
            case "not" -> validateNode(node.get("child"));
            case "if" -> {
                validateNode(node.get("if"));
                validateNode(node.get("then"));
                if (node.has("else")) validateNode(node.get("else"));
            }
            default -> throw new RuleValidationException("Unsupported rule AST node type: " + type);
        }
    }

    private boolean eval(JsonNode node, Map<String, Object> row) {
        return switch (requiredText(node, "type")) {
            case "condition" -> evalCondition(node, row);
            case "and" -> iterable(node.get("children")).stream().allMatch(child -> eval(child, row));
            case "or" -> iterable(node.get("children")).stream().anyMatch(child -> eval(child, row));
            case "not" -> !eval(node.get("child"), row);
            case "if" -> eval(node.get("if"), row) ? eval(node.get("then"), row) : (!node.has("else") || eval(node.get("else"), row));
            default -> false;
        };
    }

    private boolean evalCondition(JsonNode node, Map<String, Object> row) {
        Object actual = row.get(requiredText(node, "field"));
        String operator = requiredText(node, "operator");
        JsonNode expected = node.get("value");
        if (actual == null) return false;
        return switch (operator) {
            case "=", "==" -> Objects.equals(String.valueOf(actual), scalar(expected));
            case "!=" -> !Objects.equals(String.valueOf(actual), scalar(expected));
            case ">", ">=", "<", "<=" -> compareNumber(actual, expected, operator);
            case "IN" -> iterable(expected).stream().anyMatch(v -> Objects.equals(String.valueOf(actual), scalar(v)));
            case "NOT IN" -> iterable(expected).stream().noneMatch(v -> Objects.equals(String.valueOf(actual), scalar(v)));
            case "CONTAINS" -> String.valueOf(actual).contains(scalar(expected));
            default -> throw new RuleValidationException("Unsupported operator: " + operator);
        };
    }

    private boolean compareNumber(Object actual, JsonNode expected, String operator) {
        double a = Double.parseDouble(String.valueOf(actual));
        double b = expected.asDouble();
        return switch (operator) {
            case ">" -> a > b;
            case ">=" -> a >= b;
            case "<" -> a < b;
            case "<=" -> a <= b;
            default -> false;
        };
    }

    private String scalar(JsonNode node) {
        return node.isTextual() ? node.asText() : String.valueOf(node.isBoolean() ? node.asBoolean() : node.isNumber() ? node.numberValue() : node.asText());
    }

    private String requiredText(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) throw new RuleValidationException("Rule node missing required field: " + field);
        return node.get(field).asText();
    }

    private List<JsonNode> iterable(JsonNode node) {
        if (node == null || !node.isArray()) throw new RuleValidationException("Expected JSON array in rule AST.");
        List<JsonNode> list = new ArrayList<>();
        node.forEach(list::add);
        return list;
    }
}
