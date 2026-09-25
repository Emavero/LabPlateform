package com.labplatform.application.port.in.auth;

import java.util.Optional;

public interface RequestPasswordResetUseCase {

    /**
     * Toujours la même réponse, que le compte existe ou non. Le jeton brut
     * n'est renvoyé que si l'application est configurée en mode démo.
     */
    Result request(String email);

    record Result(String message, Optional<String> demoToken) {
    }
}
