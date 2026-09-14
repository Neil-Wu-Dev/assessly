package com.assessly.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_documents")
public class ControlDocument {
    @Id
    private UUID id;
    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;
    @Column(nullable = false)
    private String filename;
    @Column(nullable = false)
    private String format;
    private String title;
    @Column(name = "full_text", nullable = false)
    private String fullText;
    @Column(name = "structure_json", nullable = false)
    private String structureJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ControlDocument() {
    }

    public ControlDocument(UUID datasetId, String filename, String format, String title, String fullText, String structureJson) {
        if (fullText == null || fullText.isBlank()) {
            throw new IllegalArgumentException("Security Control document must contain extractable text.");
        }
        this.id = UUID.randomUUID();
        this.datasetId = datasetId;
        this.filename = filename;
        this.format = format;
        this.title = title;
        this.fullText = fullText;
        this.structureJson = structureJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDatasetId() { return datasetId; }
    public String getFilename() { return filename; }
    public String getFormat() { return format; }
    public String getTitle() { return title; }
    public String getFullText() { return fullText; }
    public String getStructureJson() { return structureJson; }
    public Instant getCreatedAt() { return createdAt; }
}
