package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.BillingDtos;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.billing.CancelSubscriptionUseCase;
import com.labplatform.application.port.in.billing.ConfirmPaymentUseCase;
import com.labplatform.application.port.in.billing.GetBillingUseCase;
import com.labplatform.application.port.in.billing.StartCheckoutUseCase;
import com.labplatform.domain.billing.PaymentMethod;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Abonnement du compte connecté, et retour des prestataires de paiement. */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final GetBillingUseCase billing;
    private final StartCheckoutUseCase startCheckout;
    private final ConfirmPaymentUseCase confirmPayment;
    private final CancelSubscriptionUseCase cancelSubscription;

    public BillingController(GetBillingUseCase billing, StartCheckoutUseCase startCheckout,
                             ConfirmPaymentUseCase confirmPayment, CancelSubscriptionUseCase cancelSubscription) {
        this.billing = billing;
        this.startCheckout = startCheckout;
        this.confirmPayment = confirmPayment;
        this.cancelSubscription = cancelSubscription;
    }

    @GetMapping
    public BillingDtos.BillingResponse mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return BillingDtos.BillingResponse.from(billing.billingOf(user.toActor()));
    }

    @PostMapping("/checkout")
    public BillingDtos.CheckoutResponse checkout(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody BillingDtos.CheckoutRequest request) {
        return BillingDtos.CheckoutResponse.from(
                startCheckout.startCheckout(user.toActor(), request.method(), request.period()));
    }

    /**
     * Appelé au retour du payeur. Le navigateur ne fait que signaler le
     * retour : c'est le prestataire, interrogé par le cas d'usage, qui dit si
     * le paiement est encaissé.
     */
    @PostMapping("/confirm")
    public BillingDtos.BillingResponse confirm(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestParam String reference) {
        return BillingDtos.BillingResponse.from(confirmPayment.confirm(user.toActor(), reference));
    }

    @DeleteMapping
    public BillingDtos.BillingResponse cancel(@AuthenticationPrincipal AuthenticatedUser user) {
        return BillingDtos.BillingResponse.from(cancelSubscription.cancel(user.toActor()));
    }

    /**
     * Notification du prestataire. Ouverte sans session — un serveur de
     * paiement n'a pas de cookie — mais pas sans authentification : le corps
     * brut est vérifié contre sa signature, et un corps mal signé est refusé.
     * <p>
     * Le corps est reçu en {@code String} et non désérialisé : la signature
     * porte sur les octets envoyés, qu'un aller-retour par un objet Java ne
     * reproduirait pas à l'identique.
     */
    @PostMapping("/webhooks/{method}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void webhook(@PathVariable PaymentMethod method, @RequestHeader HttpHeaders headers,
                        @RequestBody(required = false) String body) {
        confirmPayment.applyNotification(method, signatureOf(method, headers), body == null ? "" : body);
    }

    /**
     * Chaque prestataire nomme son en-tête de signature à sa façon. La lecture
     * passe par {@link HttpHeaders}, insensible à la casse, et retombe sur un
     * en-tête neutre pour un opérateur qui n'aurait pas le sien.
     */
    private static String signatureOf(PaymentMethod method, HttpHeaders headers) {
        String header = switch (method) {
            case CARD -> headers.getFirst("Stripe-Signature");
            case WAVE -> headers.getFirst("Wave-Signature");
            case ORANGE_MONEY -> null;
        };
        return header != null ? header : headers.getFirst("X-Payment-Signature");
    }
}
