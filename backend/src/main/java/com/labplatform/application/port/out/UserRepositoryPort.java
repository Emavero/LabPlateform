package com.labplatform.application.port.out;

import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(Email email);

    Optional<User> findByResetTokenHash(String resetTokenHash);

    boolean existsByEmail(Email email);

    User save(User user);
}
