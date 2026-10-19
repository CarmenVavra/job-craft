package com.jobcraft.dto;

import java.util.UUID;

/**
 * Öffentliche Benutzerdaten.
 */
public record UserDto(UUID id, String email, String name) {
}
