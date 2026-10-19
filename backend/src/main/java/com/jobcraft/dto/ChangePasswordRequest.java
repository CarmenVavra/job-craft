package com.jobcraft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Passwortänderung des angemeldeten Benutzers.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Bitte gib dein aktuelles Passwort ein.")
        String currentPassword,
        @NotBlank(message = "Das neue Passwort muss mindestens 8 Zeichen lang sein.")
        @Size(min = 8, max = 128, message = "Das neue Passwort muss mindestens 8 Zeichen lang sein.")
        String newPassword) {
}
