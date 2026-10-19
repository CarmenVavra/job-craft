package com.jobcraft.exception;

import org.springframework.http.HttpStatus;

/**
 * Ressource existiert nicht oder gehört nicht dem angemeldeten Benutzer.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
