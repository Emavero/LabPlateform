package com.labplatform.user;

/**
 * Rôles applicatifs. Ajouter un rôle ici (ex: ADMIN, FORMATEUR) ne demande
 * aucune modification du module security : Spring Security lit dynamiquement
 * les autorités depuis l'entité User.
 */
public enum Role {
    USER,
    ADMIN
}
