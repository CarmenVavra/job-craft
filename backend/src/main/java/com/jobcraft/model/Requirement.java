package com.jobcraft.model;

/**
 * Anforderung aus der Stellenanzeige inkl. Abgleich mit dem Bewerberprofil.
 *
 * @param text    Anforderung
 * @param matched {@code true}, wenn das Profil die Anforderung erfüllt
 */
public record Requirement(String text, boolean matched) {
}
