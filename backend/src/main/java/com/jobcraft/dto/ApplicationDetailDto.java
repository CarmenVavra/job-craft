package com.jobcraft.dto;

import com.jobcraft.model.ApplicationStatus;
import com.jobcraft.model.LetterLength;
import com.jobcraft.model.Tone;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vollständiger Zustand einer Bewerbung für Prüfschritt und Editor.
 */
public record ApplicationDetailDto(
        UUID id,
        String jobUrl,
        String jobTitle,
        String companyName,
        String contactPerson,
        String recipientAddress,
        List<RequirementDto> requirements,
        List<String> tasks,
        List<UUID> contextDocumentIds,
        Tone tone,
        LetterLength length,
        ApplicationStatus status,
        int analysisStep,
        String errorMessage,
        Instant createdAt,
        LetterMetaDto letter,
        List<CoverLetterDto> versions,
        List<ChatMessageDto> messages) {
}
