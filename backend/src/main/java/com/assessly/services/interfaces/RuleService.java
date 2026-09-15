package com.assessly.services.interfaces;

import com.assessly.models.RuleSet;

import java.util.List;
import java.util.UUID;

public interface RuleService {
    RuleSet saveManual(UUID ownerId, UUID datasetId, String rulesJson, String summaryJson);
    RuleSet generate(UUID ownerId, UUID datasetId);
    RuleSet get(UUID ownerId, UUID datasetId, UUID ruleSetId);
    RuleSet update(UUID ownerId, UUID datasetId, UUID ruleSetId, String rulesJson, String summaryJson);
    RuleSet confirm(UUID ownerId, UUID datasetId, UUID ruleSetId);
    List<RuleSet> list(UUID ownerId, UUID datasetId);
}
