package com.labplatform.domain.billing;

/**
 * Moyen de paiement choisi par l'abonné. La carte ne couvre pas l'Afrique de
 * l'Ouest, où le paiement passe par un portefeuille mobile : les deux
 * coexistent, et l'ajout d'un troisième opérateur se fait ici puis dans un
 * adaptateur, sans toucher au domaine ni aux cas d'usage.
 */
public enum PaymentMethod {

    CARD("Carte bancaire"),
    WAVE("Wave"),
    ORANGE_MONEY("Orange Money");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
