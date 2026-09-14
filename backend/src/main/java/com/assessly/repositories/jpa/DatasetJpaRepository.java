package com.assessly.repositories.jpa;

import com.assessly.models.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetJpaRepository extends JpaRepository<Dataset, UUID> {
    List<Dataset> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
    Optional<Dataset> findByIdAndOwnerId(UUID id, UUID ownerId);
}
