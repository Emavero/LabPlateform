package com.labplatform.adapter.in.web.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/** Format d'erreur unique de toute l'API. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, List<String> details) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }

    public static ApiError of(HttpStatus status, String message, String path, List<String> details) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
    }
}
