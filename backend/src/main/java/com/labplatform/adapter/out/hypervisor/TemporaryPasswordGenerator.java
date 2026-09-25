package com.labplatform.adapter.out.hypervisor;

import java.security.SecureRandom;

/**
 * Mots de passe temporaires des machines : 16 caractères sans symboles
 * ambigus (0/O, 1/l/I), faciles à recopier à la main si besoin.
 */
public final class TemporaryPasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LENGTH = 16;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
