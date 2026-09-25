package com.labplatform.application.port.in.auth;

import com.labplatform.domain.user.Role;
import com.labplatform.domain.user.User;

public record UserSummary(Long id, String email, Role role) {

    public static UserSummary of(User user) {
        return new UserSummary(user.getId(), user.getEmail().value(), user.getRole());
    }
}
