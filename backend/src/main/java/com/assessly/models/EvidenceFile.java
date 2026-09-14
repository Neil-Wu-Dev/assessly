package com.assessly.models;

import com.assessly.defs.EvidenceFormat;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_files")
public class EvidenceFile {
    @Id
    private UUID id;
    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;
    @Column(nullable = false)
    private String filename;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceFormat format;
    @Column(name = "columns_json", nullable = false)
    private String columnsJson;
    @Column(name = "rows_json", nullable = false)
    private String rowsJson;
    @Column(name = "row_count", nullable = false)
    private int rowCount;
    @Column(name = "metadata_json", nullable = false)
    private String metadataJson;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EvidenceFile() {
    }

    public EvidenceFile(UUID datasetId, String filename, EvidenceFormat format, String columnsJson, String rowsJson, int rowCount, String metadataJson) {
        if (rowCount <= 0) {
            throw new IllegalArgumentException("Evidence must contain at least one structured row.");
        }
        this.id = UUID.randomUUID();
        this.datasetId = datasetId;
        this.filename = filename;
        this.format = format;
        this.columnsJson = columnsJson;
        this.rowsJson = rowsJson;
        this.rowCount = rowCount;
        this.metadataJson = metadataJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDatasetId() { return datasetId; }
    public String getFilename() { return filename; }
    public EvidenceFormat getFormat() { return format; }
    public String getColumnsJson() { return columnsJson; }
    public String getRowsJson() { return rowsJson; }
    public int getRowCount() { return rowCount; }
    public String getMetadataJson() { return metadataJson; }
    public Instant getCreatedAt() { return createdAt; }
}
