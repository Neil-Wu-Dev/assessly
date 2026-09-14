package com.assessly.repositories.jpa;

import com.assessly.models.ControlChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ControlChunkJpaRepository extends JpaRepository<ControlChunk, UUID> {
    List<ControlChunk> findByDocumentIdOrderByChunkIndex(UUID documentId);
}
