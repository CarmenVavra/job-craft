package com.jobcraft.dto;

import com.jobcraft.model.DocumentCategory;
import jakarta.validation.constraints.NotNull;

/**
 * Änderung der Dokument-Kategorie.
 */
public record UpdateDocumentRequest(@NotNull(message = "Bitte wähle eine Kategorie.") DocumentCategory category) {
}
