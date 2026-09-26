package com.labplatform.application.port.in.billing;

import com.labplatform.domain.billing.Plan;
import com.labplatform.domain.user.Actor;

/**
 * Formule dont jouit ce compte maintenant. Exposé comme un cas d'usage à part
 * parce que d'autres cas d'usage en dépendent (le catalogue de machines, par
 * exemple) : ils s'adressent à ce contrat, pas au service qui l'implémente.
 */
public interface GetEffectivePlanUseCase {

    Plan planOf(Actor actor);
}
