package com.jobcraft.service.ai;

import java.util.List;

/**
 * KI-Funktionen von JobCraft: Eckdaten extrahieren, Anforderungen abgleichen, Anschreiben erzeugen und überarbeiten.
 */
public interface CoverLetterAi {

    /**
     * @return {@code true}, wenn ein echtes Sprachmodell genutzt wird (sonst Demo-Modus)
     */
    boolean isLanguageModel();

    /**
     * Extrahiert die Eckdaten aus dem bereinigten Text einer Stellenanzeige.
     *
     * @param jobText   bereinigter Text
     * @param pageTitle Seitentitel bzw. H1
     * @param siteName  Seitenname (z. B. og:site_name)
     * @return Eckdaten
     */
    JobExtraction extractJob(String jobText, String pageTitle, String siteName);

    /**
     * Gleicht Anforderungen mit dem Bewerberprofil ab.
     *
     * @param requirements  Anforderungen
     * @param applicantText Text aus Lebenslauf/Skill-Profil
     * @return je Anforderung (gleiche Reihenfolge) {@code true}, wenn erfüllt
     */
    List<Boolean> matchRequirements(List<String> requirements, String applicantText);

    /**
     * Erzeugt den ersten Entwurf (nur Brieftext-Absätze, ohne Anrede und Grußformel).
     *
     * @param context Kontext
     * @return Absätze
     */
    List<String> generate(GenerationContext context);

    /**
     * Überarbeitet den Brieftext gemäß Nutzerwunsch.
     *
     * @param context     Kontext
     * @param current     aktuelle Absätze
     * @param instruction Änderungswunsch
     * @return neue Fassung inkl. Antwort
     */
    LetterRevision revise(GenerationContext context, List<String> current, String instruction);
}
