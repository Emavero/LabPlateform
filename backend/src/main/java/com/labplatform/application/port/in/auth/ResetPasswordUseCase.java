package com.labplatform.application.port.in.auth;

public interface ResetPasswordUseCase {

    void reset(Command command);

    record Command(String token, String newPassword, String confirmPassword) {
    }
}
