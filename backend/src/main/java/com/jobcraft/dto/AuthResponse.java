package com.jobcraft.dto;

/**
 * Antwort auf Login/Registrierung.
 */
public record AuthResponse(String token, UserDto user) {
}
