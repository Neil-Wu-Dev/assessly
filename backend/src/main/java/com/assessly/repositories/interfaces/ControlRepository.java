package com.assessly.repositories.interfaces;

import com.assessly.models.ControlChunk;
import com.assessly.models.ControlDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ControlRepository {
    ControlDocument saveDocument(ControlDocument document);
    List<ControlChunk> saveChunks(List<ControlChunk> chunks);
    List<ControlDocument> findDocumentsByDataset(UUID datasetId);
    Optional<ControlDocument> findDocumentByIdAndDataset(UUID documentId, UUID datasetId);
    List<ControlChunk> findChunksByDocument(UUID documentId);
    void deleteDocument(ControlDocument document);
    void deleteChunksByDocument(UUID documentId);
}
