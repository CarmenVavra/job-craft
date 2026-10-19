package com.jobcraft.dto;

import com.jobcraft.model.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Zeile der Bewerbungsübersicht.
 */
public record ApplicationSummaryDto(UUID id, String companyName, String jobTitle, ApplicationStatus status,
                                    Instant createdAt, int latestVersion) {
}
