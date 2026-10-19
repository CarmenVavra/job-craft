package com.jobcraft.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Anmeldedaten.
 */
public record LoginRequest(
        @NotBlank(message = "Bitte gib eine gültige E-Mail-Adresse ein.")
        @Email(message = "Bitte gib eine gültige E-Mail-Adresse ein.")
        String email,
        @NotBlank(message = "Bitte gib dein Passwort ein.")
        String password) {
}
