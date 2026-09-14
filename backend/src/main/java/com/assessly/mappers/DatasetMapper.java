package com.assessly.mappers;

import com.assessly.models.ControlDocument;
import com.assessly.models.Dataset;
import com.assessly.models.EvidenceFile;
import com.assessly.schemas.DatasetSchemas.ControlDocumentResponse;
import com.assessly.schemas.DatasetSchemas.DatasetResponse;
import com.assessly.schemas.DatasetSchemas.EvidenceResponse;
import org.springframework.stereotype.Component;

@Component
public class DatasetMapper {
    public DatasetResponse toResponse(Dataset dataset) {
        return new DatasetResponse(dataset.getId(), dataset.getName(), dataset.getDescription(), dataset.getSchemaJson(), dataset.getCreatedAt(), dataset.getUpdatedAt());
    }

    public EvidenceResponse toResponse(EvidenceFile evidence) {
        return new EvidenceResponse(evidence.getId(), evidence.getFilename(), evidence.getFormat().name(), evidence.getRowCount(), evidence.getColumnsJson(), evidence.getRowsJson(), evidence.getCreatedAt());
    }

    public ControlDocumentResponse toResponse(ControlDocument document) {
        return new ControlDocumentResponse(document.getId(), document.getFilename(), document.getFormat(), document.getTitle(), document.getFullText(), document.getStructureJson(), document.getCreatedAt());
    }
}
