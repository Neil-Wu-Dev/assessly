package com.assessly.repositories;

import com.assessly.models.ControlChunk;
import com.assessly.models.ControlDocument;
import com.assessly.repositories.interfaces.ControlRepository;
import com.assessly.repositories.jpa.ControlChunkJpaRepository;
import com.assessly.repositories.jpa.ControlDocumentJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ControlRepositoryAdapter implements ControlRepository {
    private final ControlDocumentJpaRepository documents;
    private final ControlChunkJpaRepository chunks;

    ControlRepositoryAdapter(ControlDocumentJpaRepository documents, ControlChunkJpaRepository chunks) {
        this.documents = documents;
        this.chunks = chunks;
    }

    public ControlDocument saveDocument(ControlDocument document) { return documents.save(document); }
    public List<ControlChunk> saveChunks(List<ControlChunk> chunkList) { return chunks.saveAll(chunkList); }
    public List<ControlDocument> findDocumentsByDataset(UUID datasetId) { return documents.findByDatasetIdOrderByCreatedAtDesc(datasetId); }
    public Optional<ControlDocument> findDocumentByIdAndDataset(UUID documentId, UUID datasetId) { return documents.findByIdAndDatasetId(documentId, datasetId); }
    public List<ControlChunk> findChunksByDocument(UUID documentId) { return chunks.findByDocumentIdOrderByChunkIndex(documentId); }
    public void deleteDocument(ControlDocument document) { documents.delete(document); }
    public void deleteChunksByDocument(UUID documentId) { chunks.deleteByDocumentId(documentId); }
}
