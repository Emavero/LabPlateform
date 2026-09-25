package com.labplatform.application.port.out;

/** Génère des secrets aléatoires cryptographiquement sûrs. */
public interface SecretGeneratorPort {

    String urlSafeToken();

    /** 16 octets aléatoires en hexadécimal : le format attendu d'un flag de machine. */
    String hexToken();
}
