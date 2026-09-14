package com.assessly.repositories;

import com.assessly.models.Dataset;
import com.assessly.repositories.interfaces.DatasetRepository;
import com.assessly.repositories.jpa.DatasetJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class DatasetRepositoryAdapter implements DatasetRepository {
    private final DatasetJpaRepository jpa;

    DatasetRepositoryAdapter(DatasetJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Dataset save(Dataset dataset) { return jpa.save(dataset); }
    public List<Dataset> findByOwner(UUID ownerId) { return jpa.findByOwnerIdOrderByUpdatedAtDesc(ownerId); }
    public Optional<Dataset> findByIdAndOwner(UUID id, UUID ownerId) { return jpa.findByIdAndOwnerId(id, ownerId); }
    public void delete(Dataset dataset) { jpa.delete(dataset); }
}
