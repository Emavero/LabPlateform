package com.labplatform.adapter.in.web.dto;

import com.labplatform.application.port.in.account.UserProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record ProfileResponse(Long id, String email, String role, Instant createdAt) {

        public static ProfileResponse from(UserProfile profile) {
            return new ProfileResponse(profile.id(), profile.email(), profile.role().name(), profile.createdAt());
        }
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Le mot de passe actuel est obligatoire") @Size(max = 128) String currentPassword,
            @NotBlank(message = "Le nouveau mot de passe est obligatoire") @Size(max = 128) String newPassword,
            @NotBlank(message = "La confirmation du mot de passe est obligatoire") @Size(max = 128) String confirmPassword) {
    }
}
