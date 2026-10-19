package com.jobcraft.exception;

import org.springframework.http.HttpStatus;

/**
 * Anmeldedaten oder aktuelles Passwort sind falsch.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
