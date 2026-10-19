package com.jobcraft.dto;

import com.jobcraft.model.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Statusänderung einer Bewerbung.
 */
public record UpdateStatusRequest(@NotNull(message = "Bitte wähle einen Status.") ApplicationStatus status) {
}
