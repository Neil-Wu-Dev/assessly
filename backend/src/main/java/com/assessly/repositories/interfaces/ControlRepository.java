package com.assessly.repositories.interfaces;

import com.assessly.models.ControlChunk;
import com.assessly.models.ControlDocument;

import java.util.List;
import java.util.UUID;

public interface ControlRepository {
    ControlDocument saveDocument(ControlDocument document);
    List<ControlChunk> saveChunks(List<ControlChunk> chunks);
    List<ControlDocument> findDocumentsByDataset(UUID datasetId);
    List<ControlChunk> findChunksByDocument(UUID documentId);
}
