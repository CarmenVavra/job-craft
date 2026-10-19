package com.jobcraft.service.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Ergebnis einer Überarbeitung im Chat.
 *
 * @param reply             kurze Antwort an den Nutzer
 * @param paragraphs        vollständige neue Absätze
 * @param changedParagraphs 0-basierte Indizes der geänderten Absätze
 */
public record LetterRevision(
        @JsonPropertyDescription("Kurze, freundliche Antwort an den Nutzer (1-2 Sätze, Du-Form), was geändert wurde")
        String reply,
        @JsonPropertyDescription("Alle Absätze des überarbeiteten Brieftextes in Reihenfolge, ohne Anrede und Grußformel")
        List<String> paragraphs,
        @JsonPropertyDescription("0-basierte Indizes der Absätze, die sich gegenüber der Vorversion geändert haben")
        List<Integer> changedParagraphs) {
}
