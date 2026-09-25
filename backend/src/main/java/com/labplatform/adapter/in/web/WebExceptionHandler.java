package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.ApiError;
import com.labplatform.domain.shared.AuthenticationFailedException;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.DomainException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.ServiceUnavailableException;
import com.labplatform.domain.shared.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

/**
 * Traduction des erreurs métier en réponses HTTP. C'est le seul endroit qui
 * associe une catégorie d'exception du domaine à un code de statut.
 */
@RestControllerAdvice
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    private static final Map<Class<? extends DomainException>, HttpStatus> STATUS_BY_CATEGORY = Map.of(
            InvalidInputException.class, HttpStatus.BAD_REQUEST,
            AuthenticationFailedException.class, HttpStatus.UNAUTHORIZED,
            NotFoundException.class, HttpStatus.NOT_FOUND,
            ConflictException.class, HttpStatus.CONFLICT,
            ServiceUnavailableException.class, HttpStatus.SERVICE_UNAVAILABLE);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex, HttpServletRequest request) {
        HttpStatus status = STATUS_BY_CATEGORY.entrySet().stream()
                .filter(e -> e.getKey().isInstance(ex))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(HttpStatus.UNPROCESSABLE_ENTITY);
        return respond(status, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Données invalides");
        return respond(HttpStatus.BAD_REQUEST, message, request, details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformed(Exception ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "Requête mal formée", request, List.of());
    }

    /** Filet de sécurité : deux inscriptions simultanées avec le même e-mail. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violation de contrainte d'intégrité sur {}", request.getRequestURI(), ex);
        return respond(HttpStatus.CONFLICT, "Cette ressource existe déjà", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Exceptions Spring MVC standard (404 route inconnue, 405 méthode non supportée...) : garder leur statut.
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return respond(status, status.getReasonPhrase(), request, List.of());
            }
        }
        log.error("Erreur inattendue sur {}", request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue", request, List.of());
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String message, HttpServletRequest request,
                                                    List<String> details) {
        return ResponseEntity.status(status).body(ApiError.of(status, message, request.getRequestURI(), details));
    }
}
