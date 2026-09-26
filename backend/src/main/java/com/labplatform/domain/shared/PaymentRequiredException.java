package com.labplatform.domain.shared;

/**
 * L'appelant est bien authentifié et en droit d'agir, mais la ressource est
 * réservée aux comptes abonnés. Distinguer ce cas d'un simple refus permet à
 * l'interface de proposer l'abonnement au lieu d'afficher une erreur.
 */
public class PaymentRequiredException extends DomainException {

    public PaymentRequiredException(String message) {
        super(message);
    }
}
