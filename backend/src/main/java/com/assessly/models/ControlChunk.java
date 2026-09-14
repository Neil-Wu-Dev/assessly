package com.assessly.models;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_chunks")
public class ControlChunk {
    @Id
    private UUID id;
    @Column(name = "document_id", nullable = false)
    private UUID documentId;
    @Column(name = "control_id")
    private String controlId;
    private String section;
    private Integer page;
    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;
    @Column(name = "chunk_text", nullable = false)
    private String chunkText;
    @Column(name = "parent_reference", nullable = false)
    private String parentReference;
    @Column(columnDefinition = "vector(1536)")
    private String embedding;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ControlChunk() {
    }

    public ControlChunk(UUID documentId, String controlId, String section, Integer page, int chunkIndex, String chunkText, String parentReference) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.controlId = controlId;
        this.section = section;
        this.page = page;
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
        this.parentReference = parentReference;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public String getControlId() { return controlId; }
    public String getSection() { return section; }
    public Integer getPage() { return page; }
    public int getChunkIndex() { return chunkIndex; }
    public String getChunkText() { return chunkText; }
    public String getParentReference() { return parentReference; }
    public String getEmbedding() { return embedding; }
    public Instant getCreatedAt() { return createdAt; }
}
