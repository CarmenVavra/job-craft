package com.jobcraft.dto;

import jakarta.validation.constraints.Size;

/**
 * Eigenes Briefkopf-Feld.
 */
public record CustomFieldDto(@Size(max = 100) String label, @Size(max = 255) String value) {
}
