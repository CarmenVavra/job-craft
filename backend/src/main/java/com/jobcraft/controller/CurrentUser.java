package com.jobcraft.controller;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Liest die Benutzer-ID aus dem JWT (Subject).
 */
final class CurrentUser {

    private CurrentUser() {
    }

    static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
