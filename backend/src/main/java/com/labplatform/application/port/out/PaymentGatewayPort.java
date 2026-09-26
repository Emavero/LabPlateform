package com.labplatform.application.port.out;

import com.labplatform.domain.billing.Money;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PaymentStatus;

import java.util.Optional;

/**
 * Ce dont l'application a besoin d'un encaisseur, sans rien savoir de celui
 * qui l'assure : ouvrir une session de paiement, relire l'état d'une session,
 * et comprendre une notification signée.
 * <p>
 * Point important pour la sécurité : l'application ne décide jamais qu'un
 * paiement a réussi. Elle le demande ici, et seule la réponse de
 * l'implémentation fait foi — sans quoi il suffirait d'appeler l'URL de retour
 * pour s'offrir un abonnement.
 */
public interface PaymentGatewayPort {

    /** Ce moyen de paiement est-il servi et configuré ? */
    boolean supports(PaymentMethod method);

    /** Ouvre la session de paiement et rend l'adresse où envoyer le payeur. */
    Checkout open(CheckoutRequest request);

    /** État réel de la session chez le prestataire. */
    PaymentStatus verify(PaymentMethod method, String providerReference);

    /**
     * Lit une notification entrante. Rend {@code Optional.empty()} pour un
     * événement qui ne nous concerne pas, et lève une exception si la
     * signature ne correspond pas : un corps non signé n'est pas une
     * notification, c'est n'importe qui.
     */
    Optional<Notification> readNotification(PaymentMethod method, String signature, String rawBody);

    /**
     * @param reference  notre référence, que le prestataire nous rend telle quelle
     * @param successUrl page de retour après paiement accepté
     * @param cancelUrl  page de retour après abandon
     */
    record CheckoutRequest(String reference, Money amount, PaymentMethod method, String description,
                           String successUrl, String cancelUrl) {
    }

    record Checkout(String redirectUrl, String providerReference) {
    }

    record Notification(String reference, String providerReference, PaymentStatus status, String reason) {
    }
}
