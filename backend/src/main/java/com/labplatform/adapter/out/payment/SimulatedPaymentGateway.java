package com.labplatform.adapter.out.payment;

import com.labplatform.application.port.out.PaymentGatewayPort;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

/**
 * Encaisseur factice : accepte tout, sans appeler personne.
 * <p>
 * Il existe pour deux raisons. La première est de pouvoir dérouler et tester
 * le parcours d'abonnement de bout en bout sans compte marchand. La seconde
 * est de ne pas obliger une installation de démonstration à ouvrir un compte
 * chez un prestataire.
 * <p>
 * Conséquence à ne pas perdre de vue : dans ce mode, n'importe quel compte
 * s'offre un abonnement en cliquant. C'est pourquoi le démarrage l'annonce
 * dans les journaux, et pourquoi une installation qui facture réellement doit
 * passer {@code app.billing.mode} à {@code live}.
 */
public class SimulatedPaymentGateway implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    private final Set<PaymentMethod> methods;

    public SimulatedPaymentGateway(Set<PaymentMethod> methods) {
        this.methods = Set.copyOf(methods);
    }

    @Override
    public boolean supports(PaymentMethod method) {
        return methods.contains(method);
    }

    @Override
    public Checkout open(CheckoutRequest request) {
        log.info("Paiement simulé de {} {} par {}", request.amount().toDecimal(), request.amount().currencyCode(),
                request.method());
        // Renvoie directement sur la page de retour : le parcours est le même
        // que chez un vrai prestataire, sans étape de saisie.
        return new Checkout(request.successUrl() + "&simulated=1", "sim_" + request.reference());
    }

    @Override
    public PaymentStatus verify(PaymentMethod method, String providerReference) {
        return PaymentStatus.SUCCEEDED;
    }

    /**
     * Rien à lire : en mode simulé, c'est le retour du payeur qui conclut le
     * paiement. Accepter ici un corps non signé ouvrirait une porte à tous.
     */
    @Override
    public Optional<Notification> readNotification(PaymentMethod method, String signature, String rawBody) {
        return Optional.empty();
    }
}
