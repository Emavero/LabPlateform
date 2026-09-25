package com.labplatform.adapter.in.web.dto;

import com.labplatform.application.port.in.auth.UserSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contrats HTTP du module d'authentification. La validation de surface
 * (champs présents, tailles raisonnables) est faite ici ; les règles
 * métier (format d'e-mail, politique de mot de passe) restent dans le domaine.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "L'adresse e-mail est obligatoire") @Size(max = 254) String email,
            @NotBlank(message = "Le mot de passe est obligatoire") @Size(max = 128) String password,
            @NotBlank(message = "La confirmation du mot de passe est obligatoire") @Size(max = 128) String confirmPassword) {
    }

    public record LoginRequest(
            @NotBlank(message = "L'adresse e-mail est obligatoire") @Size(max = 254) String email,
            @NotBlank(message = "Le mot de passe est obligatoire") @Size(max = 128) String password) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "L'adresse e-mail est obligatoire") @Size(max = 254) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Le lien de réinitialisation est invalide") @Size(max = 256) String token,
            @NotBlank(message = "Le nouveau mot de passe est obligatoire") @Size(max = 128) String newPassword,
            @NotBlank(message = "La confirmation du mot de passe est obligatoire") @Size(max = 128) String confirmPassword) {
    }

    /** Le jeton n'apparaît jamais dans le corps : il voyage uniquement dans le cookie HttpOnly. */
    public record SessionResponse(UserResponse user) {
    }

    public record UserResponse(Long id, String email, String role) {

        public static UserResponse from(UserSummary summary) {
            return new UserResponse(summary.id(), summary.email(), summary.role().name());
        }
    }

    /** devResetToken n'est renseigné qu'en mode démo (app.security.expose-reset-token-in-response). */
    public record ForgotPasswordResponse(String message, String devResetToken) {
    }
}
