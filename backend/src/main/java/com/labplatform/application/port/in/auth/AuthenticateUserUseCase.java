package com.labplatform.application.port.in.auth;

public interface AuthenticateUserUseCase {

    AuthenticatedSession authenticate(Command command);

    record Command(String email, String password) {
    }
}
