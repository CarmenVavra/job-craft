package com.jobcraft.service.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

/**
 * Extrahiert Fließtext und Seitenzahl aus PDF (Apache PDFBox) und DOCX (Apache POI).
 */
@Service
public class TextExtractionService {

    private static final int WORDS_PER_PAGE = 450;

    /**
     * Ergebnis einer Extraktion.
     *
     * @param text  extrahierter Text
     * @param pages Seitenzahl
     */
    public record ExtractionResult(String text, int pages) {
    }

    /**
     * @param content  Dateiinhalt
     * @param fileType Dateityp
     * @return Text und Seitenzahl
     * @throws IOException wenn die Datei nicht gelesen werden kann
     */
    public ExtractionResult extract(byte[] content, FileType fileType) throws IOException {
        return switch (fileType) {
            case PDF -> extractPdf(content);
            case DOCX -> extractDocx(content);
        };
    }

    private ExtractionResult extractPdf(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return new ExtractionResult(normalize(stripper.getText(document)), document.getNumberOfPages());
        }
    }

    private ExtractionResult extractDocx(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = normalize(extractor.getText());
            int pages = document.getProperties().getExtendedProperties().getPages();
            if (pages <= 0) {
                int words = text.isBlank() ? 0 : text.split("\\s+").length;
                pages = Math.max(1, (int) Math.ceil(words / (double) WORDS_PER_PAGE));
            }
            return new ExtractionResult(text, pages);
        }
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n").replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").trim();
    }
}
