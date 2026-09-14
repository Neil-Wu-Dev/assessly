package com.assessly.services;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

final class RuleFieldExtractor {
    private RuleFieldExtractor() {}

    static Set<String> fields(String rulesJson) {
        Set<String> fields = new LinkedHashSet<>();
        JsonNode root = JsonSupport.readTree(rulesJson);
        JsonNode rules = root.has("rules") ? root.get("rules") : root;
        rules.forEach(rule -> collect(rule.get("ast"), fields));
        return fields;
    }

    private static void collect(JsonNode node, Set<String> fields) {
        if (node == null) return;
        if ("condition".equals(node.path("type").asText()) && node.hasNonNull("field")) fields.add(node.get("field").asText());
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isObject()) collect(value, fields);
            if (value.isArray()) value.forEach(child -> collect(child, fields));
        });
    }
}
