package com.jobcraft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Änderungswunsch an den KI-Assistenten.
 */
public record ChatRequest(
        @NotBlank(message = "Bitte gib eine Nachricht ein.")
        @Size(max = 2000, message = "Die Nachricht ist zu lang (max. 2000 Zeichen).")
        String message) {
}
