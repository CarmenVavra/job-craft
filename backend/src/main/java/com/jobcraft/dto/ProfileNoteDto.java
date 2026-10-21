package com.jobcraft.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Weitere Angabe zur Person.
 */
public record ProfileNoteDto(UUID id, String title, String content, boolean included, Instant createdAt,
                             Instant updatedAt) {
}
