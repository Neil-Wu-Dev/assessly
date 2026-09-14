package com.assessly.repositories;

import com.assessly.models.RuleSet;
import com.assessly.repositories.interfaces.RuleSetRepository;
import com.assessly.repositories.jpa.RuleSetJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class RuleSetRepositoryAdapter implements RuleSetRepository {
    private final RuleSetJpaRepository jpa;

    RuleSetRepositoryAdapter(RuleSetJpaRepository jpa) {
        this.jpa = jpa;
    }

    public RuleSet save(RuleSet ruleSet) { return jpa.save(ruleSet); }

    public int nextVersion(UUID datasetId) {
        return jpa.findFirstByDatasetIdOrderByVersionDesc(datasetId)
                .map(ruleSet -> ruleSet.getVersion() + 1)
                .orElse(1);
    }

    public Optional<RuleSet> findLatestByDataset(UUID datasetId) { return jpa.findFirstByDatasetIdOrderByVersionDesc(datasetId); }
    public Optional<RuleSet> findByIdAndDataset(UUID id, UUID datasetId) { return jpa.findByIdAndDatasetId(id, datasetId); }
    public List<RuleSet> findAllByDataset(UUID datasetId) { return jpa.findByDatasetIdOrderByVersionDesc(datasetId); }
}
