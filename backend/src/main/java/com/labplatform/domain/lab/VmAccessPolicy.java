package com.labplatform.domain.lab;

import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;

/**
 * Règle d'autorisation d'accès aux machines : un utilisateur n'opère que
 * ses propres VM ; un administrateur peut opérer toutes les VM.
 * Un refus est présenté comme une absence (404) pour ne pas révéler
 * l'existence des machines d'autrui.
 */
public final class VmAccessPolicy {

    private VmAccessPolicy() {
    }

    public static boolean canOperate(Actor actor, VirtualMachine vm) {
        return actor.isAdmin() || vm.isOwnedBy(actor.userId());
    }

    public static VirtualMachine requireAccess(Actor actor, VirtualMachine vm) {
        if (!canOperate(actor, vm)) {
            throw new NotFoundException("Machine introuvable");
        }
        return vm;
    }
}
