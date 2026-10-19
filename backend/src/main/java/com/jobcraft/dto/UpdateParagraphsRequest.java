package com.jobcraft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Manuell bearbeitete Absätze des Anschreibens (vollständige neue Fassung).
 *
 * @param paragraphs Absätze in Reihenfolge
 */
public record UpdateParagraphsRequest(
        @NotEmpty(message = "Das Anschreiben braucht mindestens einen Absatz.")
        @Size(max = 30, message = "Das Anschreiben darf höchstens 30 Absätze haben.")
        List<@NotBlank(message = "Absätze dürfen nicht leer sein.")
            @Size(max = 5000, message = "Ein Absatz darf höchstens 5000 Zeichen haben.") String> paragraphs) {
}
