package com.labplatform.common;

import org.springframework.http.HttpStatus;

/**
 * Exception métier générique. Chaque module peut lancer une BusinessException
 * (ou une sous-classe) sans avoir à écrire son propre gestionnaire d'erreurs :
 * le GlobalExceptionHandler la traduit automatiquement en ApiError.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
