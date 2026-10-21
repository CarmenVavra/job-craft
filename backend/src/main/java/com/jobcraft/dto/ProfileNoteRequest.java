package com.jobcraft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Anlegen oder Ändern einer weiteren Angabe.
 *
 * @param title    Überschrift, z. B. „Privatprojekt: Haushaltsbuch-App“
 * @param content  Beschreibung
 * @param included bei Analyse und Anschreiben berücksichtigen
 */
public record ProfileNoteRequest(
        @NotBlank(message = "Bitte gib einen Titel ein.")
        @Size(max = 120, message = "Der Titel darf höchstens 120 Zeichen lang sein.")
        String title,
        @NotBlank(message = "Bitte beschreibe die Angabe.")
        @Size(max = 4000, message = "Die Beschreibung darf höchstens 4000 Zeichen lang sein.")
        String content,
        boolean included) {
}
