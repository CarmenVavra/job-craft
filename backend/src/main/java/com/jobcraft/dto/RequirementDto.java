package com.jobcraft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Anforderung inkl. Profilabgleich.
 */
public record RequirementDto(@NotBlank @Size(max = 255) String text, boolean matched) {
}
