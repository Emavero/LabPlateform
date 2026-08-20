package com.labplatform.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "L'adresse e-mail est obligatoire")
        @Email(message = "Le format de l'adresse e-mail est invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password,

        @NotBlank(message = "La confirmation du mot de passe est obligatoire")
        String confirmPassword
) {
}
