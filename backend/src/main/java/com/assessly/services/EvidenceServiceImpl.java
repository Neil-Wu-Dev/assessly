package com.assessly.services;

import com.assessly.exceptions.NotFoundException;
import com.assessly.models.EvidenceFile;
import com.assessly.repositories.interfaces.EvidenceRepository;
import com.assessly.repositories.interfaces.RuleSetRepository;
import com.assessly.services.interfaces.DatasetService;
import com.assessly.services.interfaces.EvidenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class EvidenceServiceImpl implements EvidenceService {
    private final DatasetService datasets;
    private final EvidenceRepository evidence;
    private final RuleSetRepository ruleSets;
    private final EvidenceParser parser;

    public EvidenceServiceImpl(DatasetService datasets, EvidenceRepository evidence, RuleSetRepository ruleSets, EvidenceParser parser) {
        this.datasets = datasets;
        this.evidence = evidence;
        this.ruleSets = ruleSets;
        this.parser = parser;
    }

    @Transactional
    public EvidenceFile upload(UUID ownerId, UUID datasetId, MultipartFile file) {
        var dataset = datasets.get(ownerId, datasetId);
        ParsedEvidence parsed = parser.parse(file);
        Set<String> required = ruleSets.findLatestByDataset(datasetId).map(ruleSet -> RuleFieldExtractor.fields(ruleSet.getRulesJson())).orElse(parsed.columnNames());
        dataset.adoptOrValidateSchema(JsonSupport.write(parsed.columns()), required);
        EvidenceFile saved = new EvidenceFile(datasetId, file.getOriginalFilename(), parsed.format(), JsonSupport.write(parsed.columns()), JsonSupport.write(parsed.rows()), parsed.rows().size(), JsonSupport.write(parsed.metadata()));
        return evidence.save(saved);
    }

    public List<EvidenceFile> list(UUID ownerId, UUID datasetId) {
        datasets.get(ownerId, datasetId);
        return evidence.findByDataset(datasetId);
    }

    public EvidenceFile get(UUID ownerId, UUID datasetId, UUID evidenceId) {
        datasets.get(ownerId, datasetId);
        return evidence.findByIdAndDataset(evidenceId, datasetId).orElseThrow(() -> new NotFoundException("Evidence file not found."));
    }

    public List<Map<String, Object>> rows(UUID ownerId, UUID datasetId, UUID evidenceId, int page, int size) {
        EvidenceFile file = get(ownerId, datasetId, evidenceId);
        List<Map<String, Object>> rows = JsonSupport.read(file.getRowsJson(), new TypeReference<>() {});
        int safeSize = Math.max(1, Math.min(size, 500));
        int from = Math.max(0, page) * safeSize;
        if (from >= rows.size()) return List.of();
        int to = Math.min(rows.size(), from + safeSize);
        return rows.subList(from, to);
    }

    public void delete(UUID ownerId, UUID datasetId, UUID evidenceId) {
        evidence.delete(get(ownerId, datasetId, evidenceId));
    }
}
