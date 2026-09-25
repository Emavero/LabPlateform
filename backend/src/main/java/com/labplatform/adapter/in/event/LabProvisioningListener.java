package com.labplatform.adapter.in.event;

import com.labplatform.application.port.in.lab.ProvisionDefaultLabUseCase;
import com.labplatform.domain.user.UserRegistered;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Adaptateur d'entrée événementiel : à chaque inscription, le lab par
 * défaut de l'utilisateur est provisionné. Synchrone, donc dans la même
 * transaction que la création du compte.
 */
@Component
public class LabProvisioningListener {

    private final ProvisionDefaultLabUseCase provisionDefaultLab;

    public LabProvisioningListener(ProvisionDefaultLabUseCase provisionDefaultLab) {
        this.provisionDefaultLab = provisionDefaultLab;
    }

    @EventListener
    public void on(UserRegistered event) {
        provisionDefaultLab.provisionDefaultLab(event.userId());
    }
}
