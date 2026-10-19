package com.jobcraft.service.document;

import com.jobcraft.dto.DocumentDetailDto;
import com.jobcraft.dto.DocumentDto;
import com.jobcraft.exception.BadRequestException;
import com.jobcraft.exception.NotFoundException;
import com.jobcraft.model.Document;
import com.jobcraft.model.DocumentCategory;
import com.jobcraft.model.DocumentStatus;
import com.jobcraft.repository.DocumentRepository;
import com.jobcraft.service.storage.StorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload, Verwaltung und Löschung von Bewerbungsdokumenten.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    /** Maximale Dateigröße (10 MB). */
    public static final long MAX_SIZE = 10L * 1024 * 1024;

    private static final float PREVIEW_DPI = 60f;

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final DocumentProcessor documentProcessor;

    /**
     * Gespeicherte Datei inkl. Metadaten für den Download.
     *
     * @param fileName    Dateiname
     * @param contentType MIME-Typ
     * @param content     Inhalt
     */
    public record StoredFile(String fileName, String contentType, byte[] content) {
    }

    /**
     * @param userId Benutzer
     * @return alle Dokumente des Benutzers (älteste zuerst)
     */
    @Transactional(readOnly = true)
    public List<DocumentDto> list(UUID userId) {
        return documentRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(DocumentService::toDto).toList();
    }

    /**
     * @param userId     Benutzer
     * @param documentId Dokument
     * @return Dokument inkl. extrahiertem Text
     */
    @Transactional(readOnly = true)
    public DocumentDetailDto get(UUID userId, UUID documentId) {
        Document d = load(userId, documentId);
        return new DocumentDetailDto(d.getId(), d.getFileName(), d.getContentType(), d.getCategory(), d.getSizeBytes(),
                d.getPageCount(), d.getStatus(), d.getCreatedAt(), d.getExtractedText());
    }

    /**
     * Speichert die Datei und startet die asynchrone Textextraktion. Nicht transaktional, damit das Dokument
     * bereits festgeschrieben ist, wenn die Verarbeitung startet.
     *
     * @param userId Benutzer
     * @param file   Upload (PDF oder DOCX, max. 10 MB)
     * @return Metadaten mit Status {@code PROCESSING}
     */
    public DocumentDto upload(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Bitte wähle eine Datei aus.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("Die Datei ist größer als 10 MB.");
        }
        String fileName = sanitizeFileName(file.getOriginalFilename());
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Die Datei konnte nicht gelesen werden.");
        }
        FileType type = FileType.detect(fileName, content)
                .orElseThrow(() -> new BadRequestException("Nur PDF- oder DOCX-Dateien sind erlaubt."));

        String storageKey = userId + "/" + UUID.randomUUID() + "." + type.extension();
        storageService.store(storageKey, content, type.contentType());

        Document document = new Document();
        document.setUserId(userId);
        document.setFileName(fileName);
        document.setContentType(type.contentType());
        document.setCategory(DocumentCategoryDetector.detect(fileName));
        document.setSizeBytes(content.length);
        document.setStoragePath(storageKey);
        document.setStatus(DocumentStatus.PROCESSING);
        document = documentRepository.save(document);

        documentProcessor.process(document.getId());
        return toDto(document);
    }

    /**
     * @param userId     Benutzer
     * @param documentId Dokument
     * @param category   neue Kategorie
     * @return aktualisierte Metadaten
     */
    @Transactional
    public DocumentDto updateCategory(UUID userId, UUID documentId, DocumentCategory category) {
        Document document = load(userId, documentId);
        document.setCategory(category);
        return toDto(document);
    }

    /**
     * Löscht Datei und Metadaten.
     *
     * @param userId     Benutzer
     * @param documentId Dokument
     */
    @Transactional
    public void delete(UUID userId, UUID documentId) {
        Document document = load(userId, documentId);
        documentRepository.delete(document);
        storageService.delete(document.getStoragePath());
    }

    /**
     * @param userId     Benutzer
     * @param documentId Dokument
     * @return Originaldatei
     */
    @Transactional(readOnly = true)
    public StoredFile loadFile(UUID userId, UUID documentId) {
        Document document = load(userId, documentId);
        return new StoredFile(document.getFileName(), document.getContentType(),
                storageService.load(document.getStoragePath()));
    }

    /**
     * Rendert die erste Seite eines PDF-Dokuments als PNG für die Vorschau.
     *
     * @param userId     Benutzer
     * @param documentId Dokument
     * @return PNG-Bytes, leer für Formate ohne Seitenvorschau (DOCX)
     */
    @Transactional(readOnly = true)
    public Optional<byte[]> renderPreview(UUID userId, UUID documentId) {
        Document document = load(userId, documentId);
        if (!FileType.PDF.contentType().equals(document.getContentType())) {
            return Optional.empty();
        }
        try (PDDocument pdf = Loader.loadPDF(storageService.load(document.getStoragePath()))) {
            if (pdf.getNumberOfPages() == 0) {
                return Optional.empty();
            }
            BufferedImage image = new PDFRenderer(pdf).renderImageWithDPI(0, PREVIEW_DPI, ImageType.RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Optional.of(out.toByteArray());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Document load(UUID userId, UUID documentId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new NotFoundException("Dokument nicht gefunden."));
    }

    private static String sanitizeFileName(String original) {
        String name = original == null ? "" : Path.of(original.replace('\\', '/')).getFileName().toString();
        name = name.replaceAll("[\\p{Cntrl}\"<>|*?:]", "_").trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Ungültiger Dateiname.");
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    static DocumentDto toDto(Document d) {
        return new DocumentDto(d.getId(), d.getFileName(), d.getContentType(), d.getCategory(), d.getSizeBytes(),
                d.getPageCount(), d.getStatus(), d.getCreatedAt());
    }
}
