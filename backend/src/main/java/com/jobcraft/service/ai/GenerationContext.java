package com.jobcraft.service.ai;

import com.jobcraft.model.LetterLength;
import com.jobcraft.model.Requirement;
import com.jobcraft.model.Tone;
import java.util.List;

/**
 * Alles, was die KI für Entwurf und Überarbeitung des Anschreibens braucht.
 *
 * @param applicantName Name des Bewerbers
 * @param customFields  eigene Briefkopf-Felder als „Bezeichnung: Wert“
 * @param documents     ausgewählte Dokumente mit extrahiertem Text
 * @param notes         weitere Angaben zur Person („Über mich“), z. B. Privatprojekte
 * @param companyName   Unternehmen
 * @param jobTitle      Position
 * @param requirements  Anforderungen inkl. Abgleich
 * @param tasks         Aufgaben
 * @param jobText       bereinigter Text der Anzeige
 * @param tone          Tonalität
 * @param length        Länge
 */
public record GenerationContext(
        String applicantName,
        List<String> customFields,
        List<DocumentText> documents,
        List<NoteText> notes,
        String companyName,
        String jobTitle,
        List<Requirement> requirements,
        List<String> tasks,
        String jobText,
        Tone tone,
        LetterLength length) {

    /**
     * Dokument als KI-Kontext.
     *
     * @param name     Dateiname
     * @param category Kategorie (deutsch)
     * @param text     extrahierter Text
     */
    public record DocumentText(String name, String category, String text) {
    }

    /**
     * Weitere Angabe als KI-Kontext.
     *
     * @param title   Überschrift
     * @param content Beschreibung
     */
    public record NoteText(String title, String content) {
    }
}
