package com.assessly.services;

import com.assessly.defs.EvidenceFormat;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ParsedEvidence(EvidenceFormat format, List<Map<String, Object>> columns, List<Map<String, Object>> rows, Set<String> columnNames, Map<String, Object> metadata) {
}
