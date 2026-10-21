package com.jobcraft.dto;

import jakarta.validation.constraints.Size;

/**
 * Eigene Anrede einer Bewerbung.
 *
 * @param salutation Anrede, z. B. „Liebe Frau Kessler,“; leer oder {@code null} = automatisch
 */
public record UpdateSalutationRequest(
        @Size(max = 200, message = "Die Anrede darf höchstens 200 Zeichen lang sein.") String salutation) {
}
