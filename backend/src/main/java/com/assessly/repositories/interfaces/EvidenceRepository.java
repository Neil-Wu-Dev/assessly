package com.assessly.repositories.interfaces;

import com.assessly.models.EvidenceFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepository {
    EvidenceFile save(EvidenceFile evidenceFile);
    List<EvidenceFile> findByDataset(UUID datasetId);
    Optional<EvidenceFile> findByIdAndDataset(UUID id, UUID datasetId);
    void delete(EvidenceFile evidenceFile);
}
