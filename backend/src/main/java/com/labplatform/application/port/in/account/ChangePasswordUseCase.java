package com.labplatform.application.port.in.account;

public interface ChangePasswordUseCase {

    void changePassword(Command command);

    record Command(Long userId, String currentPassword, String newPassword, String confirmPassword) {
    }
}
