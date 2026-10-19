package com.jobcraft.repository;

import java.util.UUID;

/**
 * Projektion: höchste Anschreiben-Version je Bewerbung.
 *
 * @param applicationId Bewerbung
 * @param latestVersion höchste Versionsnummer
 */
public record LatestVersion(UUID applicationId, int latestVersion) {
}
