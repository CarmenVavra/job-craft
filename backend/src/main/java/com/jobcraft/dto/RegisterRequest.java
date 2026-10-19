package com.jobcraft.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registrierungsdaten.
 */
public record RegisterRequest(
        @NotBlank(message = "Bitte gib deinen Namen ein.")
        @Size(max = 255)
        String name,
        @NotBlank(message = "Bitte gib eine gültige E-Mail-Adresse ein.")
        @Email(message = "Bitte gib eine gültige E-Mail-Adresse ein.")
        @Size(max = 255)
        String email,
        @NotBlank(message = "Das Passwort muss mindestens 8 Zeichen lang sein.")
        @Size(min = 8, max = 128, message = "Das Passwort muss mindestens 8 Zeichen lang sein.")
        String password) {
}
