package com.labplatform.application.port.in.lab;

public interface ProvisionDefaultLabUseCase {

    /** Idempotent : ne crée que les machines du modèle qui manquent encore. */
    void provisionDefaultLab(Long userId);
}
