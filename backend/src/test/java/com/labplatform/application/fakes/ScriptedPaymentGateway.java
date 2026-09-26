package com.labplatform.application.fakes;

import com.labplatform.application.port.out.PaymentGatewayPort;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PaymentStatus;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.shared.ServiceUnavailableException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Encaisseur pilotable : le test décide de ce que répond le prestataire, y
 * compris qu'il ne répond pas. C'est ce qui permet d'éprouver l'idempotence et
 * les pannes sans réseau.
 */
public class ScriptedPaymentGateway implements PaymentGatewayPort {

    public final List<String> calls = new ArrayList<>();
    public PaymentStatus nextStatus = PaymentStatus.SUCCEEDED;
    public boolean unreachable = false;
    public boolean signatureValid = true;
    /** Référence à annoncer dans la notification ; nulle : celle de la session. */
    public String notifiedReference = null;
    public String notifiedProviderReference = null;

    @Override
    public boolean supports(PaymentMethod method) {
        return method != PaymentMethod.ORANGE_MONEY;
    }

    @Override
    public Checkout open(CheckoutRequest request) {
        calls.add("open:" + request.method() + ":" + request.amount().minorUnits());
        if (unreachable) {
            throw new ServiceUnavailableException("prestataire indisponible");
        }
        return new Checkout("https://paiement.test/" + request.reference(), "prov_" + request.reference());
    }

    @Override
    public PaymentStatus verify(PaymentMethod method, String providerReference) {
        calls.add("verify:" + providerReference);
        return nextStatus;
    }

    @Override
    public Optional<Notification> readNotification(PaymentMethod method, String signature, String rawBody) {
        if (!signatureValid) {
            throw new ForbiddenException("signature invalide");
        }
        String reference = notifiedReference != null ? notifiedReference : rawBody.trim();
        String providerReference = notifiedProviderReference != null ? notifiedProviderReference
                : "prov_" + reference;
        return Optional.of(new Notification(reference, providerReference, nextStatus, null));
    }
}
