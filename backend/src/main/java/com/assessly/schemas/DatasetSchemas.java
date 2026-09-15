package com.assessly.schemas;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatasetSchemas {
    public record DatasetRequest(@NotBlank String name, String description) {}
    public record DatasetResponse(UUID id, String name, String description, String schemaJson, Instant createdAt, Instant updatedAt) {}
    public record EvidenceResponse(UUID id, String filename, String format, int rowCount, String columnsJson, String rowsJson, Instant createdAt) {}
    public record EvidenceRowsResponse(UUID evidenceId, int page, int size, List<Map<String, Object>> rows) {}
    public record ControlDocumentResponse(UUID id, String filename, String format, String title, String fullText, String structureJson, Instant createdAt) {}
    public record ControlChunkResponse(UUID id, UUID documentId, String controlId, String section, Integer page, int chunkIndex, String chunkText, String parentReference, Instant createdAt) {}
    public record DatasetDetail(DatasetResponse dataset, List<EvidenceResponse> evidenceFiles, List<ControlDocumentResponse> controlDocuments, List<RuleSchemas.RuleSetResponse> ruleSets, List<AssessmentSchemas.AssessmentRunResponse> assessmentRuns) {}
}
