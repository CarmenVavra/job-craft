package com.jobcraft.service.document;

import com.jobcraft.model.Document;
import com.jobcraft.model.DocumentStatus;
import com.jobcraft.repository.DocumentRepository;
import com.jobcraft.service.storage.StorageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Extrahiert den Text hochgeladener Dokumente asynchron, damit der Upload sofort antwortet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessor {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final TextExtractionService textExtractionService;

    /**
     * Liest die Datei, extrahiert Text und Seitenzahl und setzt den Status auf READY bzw. FAILED.
     *
     * @param documentId Dokument
     */
    @Async
    public void process(UUID documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return;
        }
        try {
            FileType type = FileType.fromContentType(document.getContentType()).orElseThrow();
            TextExtractionService.ExtractionResult result =
                    textExtractionService.extract(storageService.load(document.getStoragePath()), type);
            document.setExtractedText(result.text());
            document.setPageCount(result.pages());
            document.setStatus(DocumentStatus.READY);
        } catch (Exception e) {
            log.warn("Textextraktion für Dokument {} fehlgeschlagen", documentId, e);
            document.setStatus(DocumentStatus.FAILED);
        }
        documentRepository.save(document);
    }
}
