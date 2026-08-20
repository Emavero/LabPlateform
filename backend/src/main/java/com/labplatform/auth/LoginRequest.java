package com.labplatform.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "L'adresse e-mail est obligatoire")
        @Email(message = "Le format de l'adresse e-mail est invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String password
) {
}
