package com.jobcraft.dto;

import java.time.Instant;
import java.util.List;

/**
 * Eine Version des Anschreibens.
 */
public record CoverLetterDto(int version, List<String> paragraphs, List<Integer> changedParagraphs,
                             Instant createdAt) {
}
