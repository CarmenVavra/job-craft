package com.jobcraft.dto;

/**
 * Laufzeitinformationen.
 *
 * @param aiEnabled {@code true}, wenn ein Sprachmodell angebunden ist (sonst Demo-Modus)
 */
public record SystemInfoDto(boolean aiEnabled) {
}
