package com.jobcraft.dto;

import com.jobcraft.model.LetterLength;
import com.jobcraft.model.Tone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Geprüfte Eckdaten und KI-Kontext für den ersten Entwurf.
 */
public record GenerateRequest(
        @NotBlank(message = "Bitte gib das Unternehmen an.") @Size(max = 255) String companyName,
        @NotBlank(message = "Bitte gib die Position an.") @Size(max = 255) String jobTitle,
        @Size(max = 255) String contactPerson,
        @Size(max = 500) String recipientAddress,
        @Valid List<RequirementDto> requirements,
        List<@Size(max = 500) String> tasks,
        List<UUID> documentIds,
        @NotNull Tone tone,
        @NotNull LetterLength length) {
}
