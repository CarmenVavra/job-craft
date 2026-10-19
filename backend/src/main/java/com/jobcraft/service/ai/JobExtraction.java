package com.jobcraft.service.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/**
 * Strukturierte Eckdaten einer Stellenanzeige.
 *
 * @param companyName      Unternehmen
 * @param jobTitle         Positionsbezeichnung
 * @param contactPerson    Ansprechpartner:in inkl. Anrede (z. B. „Frau Dr. Miriam Kessler“)
 * @param recipientAddress Postanschrift „Straße Nr, PLZ Ort“
 * @param requirements     Kernanforderungen
 * @param tasks            Aufgaben
 */
public record JobExtraction(
        @JsonPropertyDescription("Name des Unternehmens inkl. Rechtsform, z. B. 'Quantis Software GmbH'; leer, wenn unbekannt")
        String companyName,
        @JsonPropertyDescription("Positionsbezeichnung wie in der Anzeige, z. B. 'Senior Fullstack Entwickler (m/w/d)'")
        String jobTitle,
        @JsonPropertyDescription("Ansprechpartner:in mit Anrede und Titel, z. B. 'Frau Dr. Miriam Kessler'; leer, wenn keine genannt")
        String contactPerson,
        @JsonPropertyDescription("Postanschrift im Format 'Straße Hausnummer, PLZ Ort'; leer, wenn unbekannt")
        String recipientAddress,
        @JsonPropertyDescription("3 bis 8 Kernanforderungen als kurze Stichworte, z. B. 'Java 17+ & Spring Boot'")
        List<String> requirements,
        @JsonPropertyDescription("3 bis 6 Hauptaufgaben als kurze Sätze")
        List<String> tasks) {
}
