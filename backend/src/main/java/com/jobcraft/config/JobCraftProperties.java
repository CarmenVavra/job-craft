package com.jobcraft.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typisierte Konfiguration unter dem Präfix {@code jobcraft}.
 *
 * @param jwt     Token-Konfiguration
 * @param cors    erlaubte Origins
 * @param ai      KI-Anbindung
 * @param storage Dateiablage
 * @param scraper Abruf von Stellenanzeigen
 * @param pdf     PDF-Erzeugung
 */
@ConfigurationProperties(prefix = "jobcraft")
public record JobCraftProperties(Jwt jwt, Cors cors, Ai ai, Storage storage, Scraper scraper, Pdf pdf) {

    /**
     * JWT-Einstellungen.
     *
     * @param secret     HMAC-Schlüssel (mind. 32 Zeichen)
     * @param expiration Gültigkeitsdauer eines Tokens
     */
    public record Jwt(String secret, Duration expiration) {
    }

    /**
     * CORS-Einstellungen.
     *
     * @param allowedOrigins erlaubte Origins
     */
    public record Cors(List<String> allowedOrigins) {
    }

    /**
     * KI-Einstellungen.
     *
     * @param apiKey    Anthropic API-Key; leer = Demo-Modus
     * @param model     Modell-ID
     * @param maxTokens maximale Antwortlänge
     */
    public record Ai(String apiKey, String model, long maxTokens) {

        /**
         * @return {@code true}, wenn ein API-Key konfiguriert ist
         */
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * Dateiablage.
     *
     * @param type      {@code local} oder {@code s3}
     * @param localPath Basisverzeichnis für {@code local}
     * @param s3        S3/MinIO-Zugang
     */
    public record Storage(String type, String localPath, S3 s3) {
    }

    /**
     * S3-kompatibler Object Storage.
     *
     * @param endpoint        Endpoint-URL (z. B. MinIO)
     * @param region          Region
     * @param bucket          Bucket-Name
     * @param accessKey       Access-Key
     * @param secretKey       Secret-Key
     * @param pathStyleAccess Path-Style-Zugriff (für MinIO erforderlich)
     */
    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey,
                     boolean pathStyleAccess) {
    }

    /**
     * Scraper.
     *
     * @param useBrowser  Headless-Browser (Playwright) für JavaScript-lastige Seiten nutzen
     * @param timeout     Zeitlimit für den Seitenabruf
     * @param demoEnabled eingebaute Demo-Anzeige für die Beispiel-URL ausliefern
     */
    public record Scraper(boolean useBrowser, Duration timeout, boolean demoEnabled) {
    }

    /**
     * PDF.
     *
     * @param warmUp Browser beim Start im Hintergrund vorbereiten
     */
    public record Pdf(boolean warmUp) {
    }
}
