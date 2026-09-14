package com.assessly.schemas;

import com.assessly.defs.RuleGenerationStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public class RuleSchemas {
    public record RuleSetResponse(UUID id, UUID datasetId, int version, RuleGenerationStatus status, String rulesJson, String generationSummaryJson, Instant createdAt, Instant confirmedAt) {}
    public record ManualRuleSetRequest(@NotBlank String rulesJson, String generationSummaryJson) {}
}
