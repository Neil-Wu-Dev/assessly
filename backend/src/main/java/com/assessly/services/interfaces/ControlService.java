package com.assessly.services.interfaces;

import com.assessly.models.ControlDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ControlService {
    ControlDocument upload(UUID ownerId, UUID datasetId, MultipartFile file);
    List<ControlDocument> list(UUID ownerId, UUID datasetId);
}
