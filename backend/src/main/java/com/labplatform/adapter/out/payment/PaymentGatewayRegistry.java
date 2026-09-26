package com.labplatform.adapter.out.payment;

import com.labplatform.application.port.out.PaymentGatewayPort;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PaymentStatus;
import com.labplatform.domain.shared.InvalidInputException;

import java.util.List;
import java.util.Optional;

/**
 * Aiguillage entre les encaisseurs : chaque moyen de paiement a le sien, et le
 * service de facturation n'en voit qu'un.
 * <p>
 * C'est ce qui permet d'ajouter un opérateur sans toucher au domaine ni aux
 * cas d'usage : une classe de plus dans ce paquet, une ligne dans la
 * configuration, et le reste ne bouge pas.
 */
public class PaymentGatewayRegistry implements PaymentGatewayPort {

    private final List<PaymentGatewayPort> gateways;

    public PaymentGatewayRegistry(List<PaymentGatewayPort> gateways) {
        this.gateways = List.copyOf(gateways);
    }

    @Override
    public boolean supports(PaymentMethod method) {
        return gateways.stream().anyMatch(gateway -> gateway.supports(method));
    }

    @Override
    public Checkout open(CheckoutRequest request) {
        return require(request.method()).open(request);
    }

    @Override
    public PaymentStatus verify(PaymentMethod method, String providerReference) {
        return require(method).verify(method, providerReference);
    }

    @Override
    public Optional<Notification> readNotification(PaymentMethod method, String signature, String rawBody) {
        return require(method).readNotification(method, signature, rawBody);
    }

    private PaymentGatewayPort require(PaymentMethod method) {
        return gateways.stream()
                .filter(gateway -> gateway.supports(method))
                .findFirst()
                .orElseThrow(() -> new InvalidInputException("Ce moyen de paiement n'est pas configuré"));
    }
}
