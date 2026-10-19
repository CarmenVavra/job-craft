package com.jobcraft.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Basisklasse fachlicher Fehler, die mit einem definierten HTTP-Status an den Client gehen.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
