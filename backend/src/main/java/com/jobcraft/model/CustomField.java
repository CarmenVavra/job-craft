package com.jobcraft.model;

/**
 * Frei definierbares Briefkopf-Feld (z. B. „Führerschein: Klasse B“).
 *
 * @param label Bezeichnung
 * @param value Wert
 */
public record CustomField(String label, String value) {
}
