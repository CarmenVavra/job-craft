package com.jobcraft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Neue Bewerbung aus einer Stellen-URL.
 */
public record CreateApplicationRequest(
        @NotBlank(message = "Bitte gib einen gültigen Link ein (beginnend mit https://).")
        @Pattern(regexp = "^https?://\\S+\\.\\S+$",
                message = "Bitte gib einen gültigen Link ein (beginnend mit https://).")
        @Size(max = 2048)
        String url) {
}
