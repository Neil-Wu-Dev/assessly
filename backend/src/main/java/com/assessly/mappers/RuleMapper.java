package com.assessly.mappers;

import com.assessly.models.RuleSet;
import com.assessly.schemas.RuleSchemas.RuleSetResponse;
import org.springframework.stereotype.Component;

@Component
public class RuleMapper {
    public RuleSetResponse toResponse(RuleSet ruleSet) {
        return new RuleSetResponse(ruleSet.getId(), ruleSet.getDatasetId(), ruleSet.getVersion(), ruleSet.getStatus(), ruleSet.getRulesJson(), ruleSet.getGenerationSummaryJson(), ruleSet.getCreatedAt(), ruleSet.getConfirmedAt());
    }
}
