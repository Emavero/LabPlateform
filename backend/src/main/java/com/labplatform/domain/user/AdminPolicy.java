package com.labplatform.domain.user;

import com.labplatform.domain.shared.ForbiddenException;

/**
 * Seul un administrateur agit sur le contenu que les autres consultent.
 * <p>
 * La chaîne de sécurité HTTP filtre déjà /api/admin/**, mais le cas d'usage
 * le revérifie : une règle métier ne dépend pas de la configuration d'un
 * framework, et un futur appelant (tâche planifiée, autre adaptateur) passe
 * par la même porte.
 */
public final class AdminPolicy {

    private AdminPolicy() {
    }

    public static void requireAdmin(Actor actor) {
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Cette opération est réservée aux administrateurs");
        }
    }
}
