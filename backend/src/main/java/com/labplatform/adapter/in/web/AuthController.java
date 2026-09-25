package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.AuthDtos.ForgotPasswordRequest;
import com.labplatform.adapter.in.web.dto.AuthDtos.ForgotPasswordResponse;
import com.labplatform.adapter.in.web.dto.AuthDtos.LoginRequest;
import com.labplatform.adapter.in.web.dto.AuthDtos.RegisterRequest;
import com.labplatform.adapter.in.web.dto.AuthDtos.ResetPasswordRequest;
import com.labplatform.adapter.in.web.dto.AuthDtos.SessionResponse;
import com.labplatform.adapter.in.web.dto.AuthDtos.UserResponse;
import com.labplatform.adapter.in.web.security.SessionCookieManager;
import com.labplatform.application.port.in.auth.AuthenticateUserUseCase;
import com.labplatform.application.port.in.auth.AuthenticatedSession;
import com.labplatform.application.port.in.auth.RegisterUserUseCase;
import com.labplatform.application.port.in.auth.RequestPasswordResetUseCase;
import com.labplatform.application.port.in.auth.ResetPasswordUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUser;
    private final AuthenticateUserUseCase authenticateUser;
    private final RequestPasswordResetUseCase requestPasswordReset;
    private final ResetPasswordUseCase resetPassword;
    private final SessionCookieManager sessionCookies;

    public AuthController(RegisterUserUseCase registerUser, AuthenticateUserUseCase authenticateUser,
                          RequestPasswordResetUseCase requestPasswordReset, ResetPasswordUseCase resetPassword,
                          SessionCookieManager sessionCookies) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
        this.requestPasswordReset = requestPasswordReset;
        this.resetPassword = resetPassword;
        this.sessionCookies = sessionCookies;
    }

    @PostMapping("/register")
    public ResponseEntity<SessionResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticatedSession session = registerUser.register(
                new RegisterUserUseCase.Command(request.email(), request.password(), request.confirmPassword()));
        return withSessionCookie(HttpStatus.CREATED, session);
    }

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedSession session = authenticateUser.authenticate(
                new AuthenticateUserUseCase.Command(request.email(), request.password()));
        return withSessionCookie(HttpStatus.OK, session);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(SessionCookieManager.headerName(), sessionCookies.clearHeaderValue())
                .build();
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        RequestPasswordResetUseCase.Result result = requestPasswordReset.request(request.email());
        return new ForgotPasswordResponse(result.message(), result.demoToken().orElse(null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPassword.reset(new ResetPasswordUseCase.Command(
                request.token(), request.newPassword(), request.confirmPassword()));
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<SessionResponse> withSessionCookie(HttpStatus status, AuthenticatedSession session) {
        return ResponseEntity.status(status)
                .header(SessionCookieManager.headerName(), sessionCookies.issueHeaderValue(session.accessToken()))
                .body(new SessionResponse(UserResponse.from(session.user())));
    }
}
