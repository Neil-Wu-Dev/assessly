package com.assessly.repositories.interfaces;

import com.assessly.models.Dataset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepository {
    Dataset save(Dataset dataset);
    List<Dataset> findByOwner(UUID ownerId);
    Optional<Dataset> findByIdAndOwner(UUID id, UUID ownerId);
    void delete(Dataset dataset);
}
