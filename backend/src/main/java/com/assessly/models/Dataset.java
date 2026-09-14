package com.assessly.models;

import com.assessly.exceptions.SchemaMismatchException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "datasets")
public class Dataset {
    @Id
    private UUID id;
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    @Column(nullable = false, length = 160)
    private String name;
    private String description;
    @Column(name = "schema_json")
    private String schemaJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Dataset() {
    }

    public Dataset(UUID ownerId, String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Dataset name is required.");
        }
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.name = name.trim();
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void adoptOrValidateSchema(String incomingSchemaJson, Set<String> requiredFields) {
        if (schemaJson == null || schemaJson.isBlank()) {
            schemaJson = incomingSchemaJson;
            touch();
            return;
        }
        Set<String> existing = extractColumnNames(schemaJson);
        if (!existing.containsAll(requiredFields)) {
            throw new SchemaMismatchException("Schema mismatch: incoming data does not contain fields required by the current dataset rules.");
        }
    }

    private Set<String> extractColumnNames(String json) {
        Set<String> names = new LinkedHashSet<>();
        for (String part : json.split("\"name\"\\s*:\\s*\"")) {
            int end = part.indexOf('"');
            if (end > 0) {
                names.add(part.substring(0, end));
            }
        }
        return names;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSchemaJson() { return schemaJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
