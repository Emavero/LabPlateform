package com.labplatform.application.port.in.auth;

public interface RegisterUserUseCase {

    AuthenticatedSession register(Command command);

    record Command(String email, String password, String confirmPassword) {
    }
}
