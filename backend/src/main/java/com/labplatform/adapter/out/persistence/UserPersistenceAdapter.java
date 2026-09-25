package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.UserJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataUserRepository;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.PasswordResetToken;
import com.labplatform.domain.user.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository repository;

    public UserPersistenceAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return repository.findByEmail(email.value()).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<User> findByResetTokenHash(String resetTokenHash) {
        return repository.findByResetTokenHash(resetTokenHash).map(UserPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return repository.existsByEmail(email.value());
    }

    @Override
    public User save(User user) {
        return toDomain(repository.save(toEntity(user)));
    }

    private static UserJpaEntity toEntity(User user) {
        Optional<PasswordResetToken> token = user.getResetToken();
        return new UserJpaEntity(
                user.getId(),
                user.getEmail().value(),
                user.getPasswordHash(),
                user.getRole(),
                user.getCreatedAt(),
                token.map(PasswordResetToken::hash).orElse(null),
                token.map(PasswordResetToken::expiresAt).orElse(null));
    }

    private static User toDomain(UserJpaEntity entity) {
        PasswordResetToken token = entity.getResetTokenHash() == null || entity.getResetTokenExpiresAt() == null
                ? null
                : new PasswordResetToken(entity.getResetTokenHash(), entity.getResetTokenExpiresAt());
        return User.restore(
                entity.getId(),
                Email.of(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getCreatedAt(),
                token);
    }
}
