package com.jobcraft.exception;

import org.springframework.http.HttpStatus;

/**
 * Fehler eines externen Dienstes (Stellenanzeige nicht abrufbar, KI-Dienst, PDF-Rendering, Storage).
 */
public class ExternalServiceException extends ApiException {

    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }

    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
