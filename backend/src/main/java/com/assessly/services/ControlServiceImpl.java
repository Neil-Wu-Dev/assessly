package com.assessly.services;

import com.assessly.models.ControlChunk;
import com.assessly.models.ControlDocument;
import com.assessly.repositories.interfaces.ControlRepository;
import com.assessly.services.interfaces.ControlService;
import com.assessly.services.interfaces.DatasetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class ControlServiceImpl implements ControlService {
    private final DatasetService datasets;
    private final ControlRepository controls;
    private final ControlDocumentParser parser;

    public ControlServiceImpl(DatasetService datasets, ControlRepository controls, ControlDocumentParser parser) {
        this.datasets = datasets;
        this.controls = controls;
        this.parser = parser;
    }

    @Transactional
    public ControlDocument upload(UUID ownerId, UUID datasetId, MultipartFile file) {
        datasets.get(ownerId, datasetId);
        var parsed = parser.parse(file);
        ControlDocument document = controls.saveDocument(new ControlDocument(datasetId, file.getOriginalFilename(), parsed.format(), parsed.title(), parsed.fullText(), JsonSupport.write(parsed.structure())));
        controls.saveChunks(parsed.chunks().stream()
                .map(chunk -> new ControlChunk(document.getId(), chunk.controlId(), chunk.section(), chunk.page(), chunk.chunkIndex(), chunk.chunkText(), chunk.parentReference()))
                .toList());
        return document;
    }

    public List<ControlDocument> list(UUID ownerId, UUID datasetId) {
        datasets.get(ownerId, datasetId);
        return controls.findDocumentsByDataset(datasetId);
    }
}
