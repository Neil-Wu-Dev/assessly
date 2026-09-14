package com.assessly.services;

import com.assessly.exceptions.UnsupportedEvidenceException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ControlDocumentParser {
    private static final Pattern CONTROL_ID = Pattern.compile("\\b([A-Z]{2,8}[-_ ]?\\d{1,4})\\b");

    public ParsedControl parse(MultipartFile file) {
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("controls.txt");
        String lower = filename.toLowerCase(Locale.ROOT);
        try {
            String text;
            String format;
            if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
                text = new String(file.getBytes());
                format = lower.endsWith(".txt") ? "TXT" : "MARKDOWN";
            } else if (lower.endsWith(".html") || lower.endsWith(".htm")) {
                text = Jsoup.parse(new String(file.getBytes())).text();
                format = "HTML";
            } else if (lower.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
                    text = String.join("\n", document.getParagraphs().stream().map(p -> p.getText()).toList());
                }
                format = "DOCX";
            } else if (lower.endsWith(".pdf")) {
                try (var document = Loader.loadPDF(file.getBytes())) {
                    text = new PDFTextStripper().getText(document);
                }
                format = "PDF";
            } else {
                throw new UnsupportedEvidenceException("Unsupported Security Control format. Supported formats: PDF, DOCX, TXT, Markdown, HTML.");
            }
            if (text == null || text.isBlank()) throw new UnsupportedEvidenceException("Security Control document has no extractable text.");
            return new ParsedControl(format, titleFrom(text), text, structure(text), chunks(text));
        } catch (UnsupportedEvidenceException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsupportedEvidenceException("Unable to extract Security Control text.", e);
        }
    }

    private String titleFrom(String text) {
        return Arrays.stream(text.split("\\R")).map(String::trim).filter(line -> !line.isBlank()).findFirst().orElse("Security Controls");
    }

    private List<Map<String, Object>> structure(String text) {
        List<Map<String, Object>> sections = new ArrayList<>();
        int index = 0;
        for (String para : text.split("\\R\\s*\\R")) {
            if (!para.isBlank()) sections.add(Map.of("paragraph", index++, "controlId", controlId(para), "text", para.trim()));
        }
        return sections;
    }

    private List<ParsedChunk> chunks(String text) {
        List<ParsedChunk> chunks = new ArrayList<>();
        int index = 0;
        for (String para : text.split("\\R\\s*\\R")) {
            String trimmed = para.trim();
            if (trimmed.isBlank()) continue;
            String controlId = controlId(trimmed);
            for (String child : childChunks(trimmed, 1200, 160)) {
                chunks.add(new ParsedChunk(controlId, null, null, index++, child, controlId == null ? "paragraph-" + index : controlId));
            }
        }
        return chunks;
    }

    private List<String> childChunks(String text, int max, int overlap) {
        if (text.length() <= max) return List.of(text);
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + max);
            parts.add(text.substring(start, end));
            if (end == text.length()) break;
            start = Math.max(0, end - overlap);
        }
        return parts;
    }

    private String controlId(String text) {
        Matcher matcher = CONTROL_ID.matcher(text);
        return matcher.find() ? matcher.group(1).replace(' ', '-') : null;
    }

    public record ParsedControl(String format, String title, String fullText, List<Map<String, Object>> structure, List<ParsedChunk> chunks) {}
    public record ParsedChunk(String controlId, String section, Integer page, int chunkIndex, String chunkText, String parentReference) {}
}
