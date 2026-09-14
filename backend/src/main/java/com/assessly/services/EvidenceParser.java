package com.assessly.services;

import com.assessly.defs.EvidenceFormat;
import com.assessly.exceptions.UnsupportedEvidenceException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStreamReader;
import java.util.*;

@Component
public class EvidenceParser {
    public ParsedEvidence parse(MultipartFile file) {
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("evidence");
        EvidenceFormat format = detect(name);
        try {
            return switch (format) {
                case CSV -> parseCsv(file);
                case XLSX -> parseXlsx(file);
                case JSON -> parseJson(file);
                case JSONL -> parseJsonl(file);
                case XML -> parseXml(file);
            };
        } catch (UnsupportedEvidenceException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsupportedEvidenceException("Unable to construct structured dataset from " + format + " evidence.", e);
        }
    }

    private EvidenceFormat detect(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) return EvidenceFormat.CSV;
        if (lower.endsWith(".xlsx")) return EvidenceFormat.XLSX;
        if (lower.endsWith(".json")) return EvidenceFormat.JSON;
        if (lower.endsWith(".jsonl")) return EvidenceFormat.JSONL;
        if (lower.endsWith(".xml")) return EvidenceFormat.XML;
        throw new UnsupportedEvidenceException("Unsupported evidence format. Supported formats: CSV, XLSX, JSON, JSONL, XML.");
    }

    private ParsedEvidence parseCsv(MultipartFile file) throws Exception {
        List<Map<String, Object>> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
                .parse(new InputStreamReader(file.getInputStream()))
                .stream().map(r -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.putAll(r.toMap());
                    return row;
                }).toList();
        return structured(EvidenceFormat.CSV, records, Map.of("parser", "commons-csv"));
    }

    private ParsedEvidence parseXlsx(MultipartFile file) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) throw new UnsupportedEvidenceException("XLSX sheet has no header row.");
            List<String> names = new ArrayList<>();
            header.forEach(cell -> names.add(cell.getStringCellValue()));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, Object> value = new LinkedHashMap<>();
                for (int c = 0; c < names.size(); c++) value.put(names.get(c), cellValue(row.getCell(c)));
                rows.add(value);
            }
            return structured(EvidenceFormat.XLSX, rows, Map.of("sheet", sheet.getSheetName()));
        }
    }

    private ParsedEvidence parseJson(MultipartFile file) throws Exception {
        JsonNode root = JsonSupport.MAPPER.readTree(file.getInputStream());
        JsonNode array = root.isArray() ? root : root.get("rows");
        if (array == null || !array.isArray()) {
            throw new UnsupportedEvidenceException("JSON evidence must be an array of objects or an object with a rows array.");
        }
        return structured(EvidenceFormat.JSON, rowsFromArray(array), Map.of("parser", "jackson"));
    }

    private ParsedEvidence parseJsonl(MultipartFile file) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String line : new String(file.getBytes()).split("\\R")) {
            if (!line.isBlank()) rows.add(JsonSupport.MAPPER.convertValue(JsonSupport.MAPPER.readTree(line), Map.class));
        }
        return structured(EvidenceFormat.JSONL, rows, Map.of("parser", "jackson-jsonl"));
    }

    private ParsedEvidence parseXml(MultipartFile file) throws Exception {
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.getInputStream());
        NodeList nodes = document.getDocumentElement().getChildNodes();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element element) {
                Map<String, Object> row = new LinkedHashMap<>();
                NodeList children = element.getChildNodes();
                for (int c = 0; c < children.getLength(); c++) {
                    if (children.item(c) instanceof Element child) row.put(child.getTagName(), child.getTextContent());
                }
                if (!row.isEmpty()) rows.add(row);
            }
        }
        return structured(EvidenceFormat.XML, rows, Map.of("root", document.getDocumentElement().getTagName()));
    }

    private List<Map<String, Object>> rowsFromArray(JsonNode array) {
        List<Map<String, Object>> rows = new ArrayList<>();
        array.forEach(node -> {
            if (!node.isObject()) throw new UnsupportedEvidenceException("Structured JSON evidence rows must be objects.");
            rows.add(JsonSupport.MAPPER.convertValue(node, Map.class));
        });
        return rows;
    }

    private ParsedEvidence structured(EvidenceFormat format, List<Map<String, Object>> rows, Map<String, Object> metadata) {
        if (rows.isEmpty()) throw new UnsupportedEvidenceException("Evidence must contain at least one structured row.");
        LinkedHashSet<String> names = new LinkedHashSet<>(rows.get(0).keySet());
        if (names.size() < 2) throw new UnsupportedEvidenceException("Unable to construct a useful column/value table from the uploaded file.");
        List<Map<String, Object>> columns = names.stream().map(name -> Map.<String, Object>of("name", name, "type", inferType(rows, name))).toList();
        return new ParsedEvidence(format, columns, rows, names, metadata);
    }

    private String inferType(List<Map<String, Object>> rows, String name) {
        return rows.stream().map(row -> row.get(name)).filter(Objects::nonNull).findFirst().map(value -> {
            if (value instanceof Boolean) return "boolean";
            if (value instanceof Number) return "number";
            return "string";
        }).orElse("string");
    }

    private Object cellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getLocalDateTimeCellValue().toLocalDate().toString() : cell.getNumericCellValue();
            case STRING -> cell.getStringCellValue();
            default -> null;
        };
    }
}

