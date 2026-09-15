package com.assessly.repositories.jpa;

import com.assessly.models.BenchmarkCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BenchmarkCaseJpaRepository extends JpaRepository<BenchmarkCase, UUID> {
    @Query("select c from BenchmarkCase c where c.builtin = true or c.ownerId = :ownerId order by c.createdAt desc")
    List<BenchmarkCase> findVisibleCases(@Param("ownerId") UUID ownerId);

    @Query("select c from BenchmarkCase c where c.id = :id and (c.builtin = true or c.ownerId = :ownerId)")
    Optional<BenchmarkCase> findVisibleById(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    Optional<BenchmarkCase> findByIdAndOwnerId(UUID id, UUID ownerId);
}
