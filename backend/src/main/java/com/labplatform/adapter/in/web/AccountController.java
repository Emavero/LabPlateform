package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.AccountDtos.ChangePasswordRequest;
import com.labplatform.adapter.in.web.dto.AccountDtos.ProfileResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.account.ChangePasswordUseCase;
import com.labplatform.application.port.in.account.GetUserProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class AccountController {

    private final GetUserProfileUseCase getUserProfile;
    private final ChangePasswordUseCase changePassword;

    public AccountController(GetUserProfileUseCase getUserProfile, ChangePasswordUseCase changePassword) {
        this.getUserProfile = getUserProfile;
        this.changePassword = changePassword;
    }

    /** Sert aussi de vérification de session au chargement du frontend. */
    @GetMapping
    public ProfileResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ProfileResponse.from(getUserProfile.getProfile(user.id()));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        changePassword.changePassword(new ChangePasswordUseCase.Command(
                user.id(), request.currentPassword(), request.newPassword(), request.confirmPassword()));
        return ResponseEntity.noContent().build();
    }
}
