package com.labplatform.application.service;

import com.labplatform.application.port.in.account.ChangePasswordUseCase;
import com.labplatform.application.port.in.account.GetUserProfileUseCase;
import com.labplatform.application.port.in.account.UserProfile;
import com.labplatform.application.port.out.PasswordHasherPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.PasswordPolicy;
import com.labplatform.domain.user.User;

/** Cas d'usage liés au compte de l'utilisateur connecté. */
public class AccountService implements GetUserProfileUseCase, ChangePasswordUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final TransactionPort transactions;

    public AccountService(UserRepositoryPort users, PasswordHasherPort passwordHasher, TransactionPort transactions) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.transactions = transactions;
    }

    @Override
    public UserProfile getProfile(Long userId) {
        User user = requireUser(userId);
        return new UserProfile(user.getId(), user.getEmail().value(), user.getRole(), user.getCreatedAt());
    }

    @Override
    public void changePassword(ChangePasswordUseCase.Command command) {
        PasswordPolicy.validateNewPassword(command.newPassword(), command.confirmPassword());

        transactions.inTransaction(() -> {
            User user = requireUser(command.userId());
            if (command.currentPassword() == null
                    || !passwordHasher.matches(command.currentPassword(), user.getPasswordHash())) {
                throw new InvalidInputException("Le mot de passe actuel est incorrect");
            }
            user.changePasswordHash(passwordHasher.hash(command.newPassword()));
            users.save(user);
        });
    }

    private User requireUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }
}
