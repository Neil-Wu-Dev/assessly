package com.assessly.services.interfaces;

import com.assessly.models.Dataset;

import java.util.List;
import java.util.UUID;

public interface DatasetService {
    Dataset create(UUID ownerId, String name, String description);
    Dataset update(UUID ownerId, UUID datasetId, String name, String description);
    List<Dataset> list(UUID ownerId);
    Dataset get(UUID ownerId, UUID datasetId);
    void delete(UUID ownerId, UUID datasetId);
}
