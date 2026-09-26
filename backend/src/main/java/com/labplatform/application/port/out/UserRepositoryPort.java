package com.labplatform.application.port.out;

import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(Email email);

    Optional<User> findByResetTokenHash(String resetTokenHash);

    boolean existsByEmail(Email email);

    User save(User user);

    /** Nombre de comptes, pour le tableau de bord d'administration. */
    long count();

    /**
     * Tous les comptes. Réservé aux indicateurs d'administration, qui ont
     * besoin des dates d'inscription pour établir les cohortes : la taille de
     * la plateforme rend une lecture complète raisonnable, et la découper en
     * pages ne ferait que déplacer le coût.
     */
    List<User> findAll();

    /** Comptes créés depuis cette date : acquisition de la période. */
    long countCreatedSince(Instant since);
}
