package com.assessly.repositories.jpa;

import com.assessly.models.BenchmarkRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BenchmarkRunJpaRepository extends JpaRepository<BenchmarkRun, UUID> {
    List<BenchmarkRun> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Optional<BenchmarkRun> findByIdAndOwnerId(UUID id, UUID ownerId);
}
