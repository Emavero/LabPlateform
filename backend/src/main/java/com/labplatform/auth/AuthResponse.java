package com.labplatform.auth;

public record AuthResponse(
        String token,
        String email,
        String role
) {
}
