package com.assessly.repositories;

import com.assessly.models.EvidenceFile;
import com.assessly.repositories.interfaces.EvidenceRepository;
import com.assessly.repositories.jpa.EvidenceJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class EvidenceRepositoryAdapter implements EvidenceRepository {
    private final EvidenceJpaRepository jpa;

    EvidenceRepositoryAdapter(EvidenceJpaRepository jpa) {
        this.jpa = jpa;
    }

    public EvidenceFile save(EvidenceFile evidenceFile) { return jpa.save(evidenceFile); }
    public List<EvidenceFile> findByDataset(UUID datasetId) { return jpa.findByDatasetIdOrderByCreatedAtDesc(datasetId); }
    public Optional<EvidenceFile> findByIdAndDataset(UUID id, UUID datasetId) { return jpa.findByIdAndDatasetId(id, datasetId); }
    public void delete(EvidenceFile evidenceFile) { jpa.delete(evidenceFile); }
}
