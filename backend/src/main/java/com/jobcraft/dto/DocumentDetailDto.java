package com.jobcraft.dto;

import com.jobcraft.model.DocumentCategory;
import com.jobcraft.model.DocumentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Dokument inkl. extrahiertem Text (Vorschau).
 */
public record DocumentDetailDto(UUID id, String fileName, String contentType, DocumentCategory category,
                                long sizeBytes, Integer pageCount, DocumentStatus status, Instant createdAt,
                                String extractedText) {
}
