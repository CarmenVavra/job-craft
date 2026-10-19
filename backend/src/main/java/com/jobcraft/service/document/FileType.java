package com.jobcraft.service.document;

import java.util.Locale;
import java.util.Optional;

/**
 * Unterstützte Upload-Formate inkl. Erkennung über Dateiendung und Signatur (Magic Bytes).
 */
public enum FileType {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final String extension;
    private final String contentType;

    FileType(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /**
     * Bestimmt den Typ anhand der Endung und prüft, ob der Inhalt dazu passt.
     *
     * @param fileName Dateiname
     * @param content  Inhalt
     * @return Typ, sofern Endung und Signatur übereinstimmen
     */
    public static Optional<FileType> detect(String fileName, byte[] content) {
        if (fileName == null || content == null || content.length < 4) {
            return Optional.empty();
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf") && content[0] == '%' && content[1] == 'P' && content[2] == 'D' && content[3] == 'F') {
            return Optional.of(PDF);
        }
        if (lower.endsWith(".docx") && content[0] == 'P' && content[1] == 'K') {
            return Optional.of(DOCX);
        }
        return Optional.empty();
    }

    /**
     * @param contentType MIME-Typ
     * @return Typ zum MIME-Typ
     */
    public static Optional<FileType> fromContentType(String contentType) {
        for (FileType type : values()) {
            if (type.contentType.equals(contentType)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
