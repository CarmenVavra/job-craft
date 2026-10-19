package com.jobcraft.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Einheitliches Fehlerformat aller API-Antworten.
 *
 * @param timestamp   Zeitpunkt
 * @param status      HTTP-Status
 * @param error       Kurzbezeichnung des Status
 * @param message     verständliche Fehlermeldung (deutsch)
 * @param fieldErrors Validierungsfehler je Feld (leer, wenn keine)
 */
public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> fieldErrors) {
}
