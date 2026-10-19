package com.jobcraft.exception;

import org.springframework.http.HttpStatus;

/**
 * Ungültige Eingabe des Clients.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
