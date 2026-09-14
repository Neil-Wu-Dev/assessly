package com.assessly.repositories.jpa;

import com.assessly.models.RuleSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleSetJpaRepository extends JpaRepository<RuleSet, UUID> {
    List<RuleSet> findByDatasetIdOrderByVersionDesc(UUID datasetId);
    Optional<RuleSet> findFirstByDatasetIdOrderByVersionDesc(UUID datasetId);
    Optional<RuleSet> findByIdAndDatasetId(UUID id, UUID datasetId);
}
