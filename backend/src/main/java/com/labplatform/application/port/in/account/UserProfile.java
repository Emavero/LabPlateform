package com.labplatform.application.port.in.account;

import com.labplatform.domain.user.Role;

import java.time.Instant;

public record UserProfile(Long id, String email, Role role, Instant createdAt) {
}
