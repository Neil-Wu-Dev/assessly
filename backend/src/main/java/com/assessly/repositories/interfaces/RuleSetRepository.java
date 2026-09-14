package com.assessly.repositories.interfaces;

import com.assessly.models.RuleSet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleSetRepository {
    RuleSet save(RuleSet ruleSet);
    int nextVersion(UUID datasetId);
    Optional<RuleSet> findLatestByDataset(UUID datasetId);
    Optional<RuleSet> findByIdAndDataset(UUID id, UUID datasetId);
    List<RuleSet> findAllByDataset(UUID datasetId);
}
