package com.labplatform.application.fakes;

import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.PasswordResetToken;
import com.labplatform.domain.user.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUsers implements UserRepositoryPort {

    private final Map<Long, User> store = new HashMap<>();
    private long sequence = 0;

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return store.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
    }

    @Override
    public Optional<User> findByResetTokenHash(String hash) {
        return store.values().stream()
                .filter(u -> u.getResetToken().map(PasswordResetToken::hash).filter(hash::equals).isPresent())
                .findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
        Long id = user.getId() != null ? user.getId() : ++sequence;
        User stored = User.restore(id, user.getEmail(), user.getPasswordHash(), user.getRole(),
                user.getCreatedAt(), user.getResetToken().orElse(null));
        store.put(id, stored);
        return stored;
    }
}
