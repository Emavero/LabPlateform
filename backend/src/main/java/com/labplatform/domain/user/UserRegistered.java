package com.labplatform.domain.user;

/**
 * Événement de domaine publié à la création d'un compte. Les autres
 * contextes (lab, notifications...) y réagissent sans que l'inscription
 * n'ait à les connaître.
 */
public record UserRegistered(Long userId, Email email) {
}
