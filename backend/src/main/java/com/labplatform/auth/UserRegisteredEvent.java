package com.labplatform.auth;

/**
 * Événement publié après la création d'un compte utilisateur.
 * Découple totalement le module auth des modules qui réagissent à une
 * inscription (ex: lab, qui provisionne des VM). Un nouveau module peut
 * s'abonner à cet événement sans que auth n'ait à le connaître.
 */
public record UserRegisteredEvent(Long userId, String email) {
}
