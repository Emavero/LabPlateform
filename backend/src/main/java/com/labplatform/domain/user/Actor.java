package com.labplatform.domain.user;

import java.util.Objects;

/**
 * Identité de l'appelant d'un cas d'usage (qui agit ?). Construite par
 * l'adaptateur d'entrée à partir du jeton de session.
 */
public record Actor(Long userId, Role role) {

    public Actor {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
