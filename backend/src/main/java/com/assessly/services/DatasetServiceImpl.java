package com.assessly.services;

import com.assessly.exceptions.NotFoundException;
import com.assessly.models.Dataset;
import com.assessly.repositories.interfaces.DatasetRepository;
import com.assessly.services.interfaces.DatasetService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DatasetServiceImpl implements DatasetService {
    private final DatasetRepository datasets;

    public DatasetServiceImpl(DatasetRepository datasets) {
        this.datasets = datasets;
    }

    public Dataset create(UUID ownerId, String name, String description) {
        return datasets.save(new Dataset(ownerId, name, description));
    }

    public List<Dataset> list(UUID ownerId) {
        return datasets.findByOwner(ownerId);
    }

    public Dataset get(UUID ownerId, UUID datasetId) {
        return datasets.findByIdAndOwner(datasetId, ownerId).orElseThrow(() -> new NotFoundException("Dataset not found."));
    }

    public void delete(UUID ownerId, UUID datasetId) {
        datasets.delete(get(ownerId, datasetId));
    }
}
