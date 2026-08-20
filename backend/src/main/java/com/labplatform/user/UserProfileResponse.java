package com.labplatform.user;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String email,
        String role,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getRole().name(), user.getCreatedAt());
    }
}
