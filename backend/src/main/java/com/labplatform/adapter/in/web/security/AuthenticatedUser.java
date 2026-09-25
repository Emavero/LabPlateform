package com.labplatform.adapter.in.web.security;

import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;

/** Principal placé dans le SecurityContext une fois le jeton de session vérifié. */
public record AuthenticatedUser(Long id, String email, Role role) {

    public Actor toActor() {
        return new Actor(id, role);
    }
}
