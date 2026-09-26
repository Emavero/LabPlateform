package com.labplatform.domain.billing;

import com.labplatform.domain.shared.PaymentRequiredException;
import com.labplatform.domain.user.Actor;

/**
 * Porte unique de l'accès Pro.
 * <p>
 * Le contrôle est ici, dans le domaine, et non dans la chaîne HTTP : une
 * ressource réservée le reste quel que soit l'appelant (contrôleur, tâche
 * planifiée, futur adaptateur), et la règle se lit en un endroit.
 * <p>
 * L'administrateur passe : il publie le contenu, il ne s'abonne pas à sa
 * propre plateforme.
 */
public final class ProAccessPolicy {

    private ProAccessPolicy() {
    }

    public static boolean granted(Actor actor, Plan effectivePlan) {
        return actor.isAdmin() || effectivePlan.isPro();
    }

    public static void requirePro(Actor actor, Plan effectivePlan) {
        if (!granted(actor, effectivePlan)) {
            throw new PaymentRequiredException("Cette machine est réservée aux abonnés Pro");
        }
    }
}
