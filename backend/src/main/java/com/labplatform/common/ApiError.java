package com.labplatform.common;

import java.time.Instant;
import java.util.List;

/**
 * Format d'erreur générique et uniforme renvoyé par toute l'API.
 * Utilisé par tous les modules (auth, lab, ...) via le GlobalExceptionHandler,
 * ce qui évite de dupliquer la logique de formatage d'erreur module par module.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }

    public static ApiError of(int status, String error, String message, String path, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, path, details);
    }
}
