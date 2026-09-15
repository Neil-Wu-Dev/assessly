package com.assessly.services.interfaces;

import com.assessly.models.EvidenceFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EvidenceService {
    EvidenceFile upload(UUID ownerId, UUID datasetId, MultipartFile file);
    List<EvidenceFile> list(UUID ownerId, UUID datasetId);
    EvidenceFile get(UUID ownerId, UUID datasetId, UUID evidenceId);
    List<Map<String, Object>> rows(UUID ownerId, UUID datasetId, UUID evidenceId, int page, int size);
    void delete(UUID ownerId, UUID datasetId, UUID evidenceId);
}
