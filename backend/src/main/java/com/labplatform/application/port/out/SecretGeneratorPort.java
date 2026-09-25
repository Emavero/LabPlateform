package com.labplatform.application.port.out;

/** Génère des secrets aléatoires cryptographiquement sûrs. */
public interface SecretGeneratorPort {

    String urlSafeToken();
}
