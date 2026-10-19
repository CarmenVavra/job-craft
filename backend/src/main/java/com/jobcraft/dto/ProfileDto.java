package com.jobcraft.dto;

import com.jobcraft.model.LetterStyle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Briefkopf-Stammdaten (Lesen und Schreiben).
 */
public record ProfileDto(
        @NotBlank(message = "Bitte gib deinen vollständigen Namen ein.")
        @Size(max = 255)
        String fullName,
        @Size(max = 255) String street,
        @Size(max = 20) String zipCode,
        @Size(max = 100) String city,
        @Size(max = 50) String phone,
        @Size(max = 255) String email,
        @Size(max = 255) String linkedin,
        @Size(max = 255) String github,
        @NotNull LetterStyle letterStyle,
        @Valid @Size(max = 30) List<CustomFieldDto> customFields) {
}
