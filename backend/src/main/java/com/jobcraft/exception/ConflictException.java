package com.jobcraft.exception;

import org.springframework.http.HttpStatus;

/**
 * Konflikt mit dem aktuellen Zustand (z. B. E-Mail bereits registriert).
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
