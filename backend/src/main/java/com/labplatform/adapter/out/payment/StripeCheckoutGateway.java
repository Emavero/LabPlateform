package com.labplatform.adapter.out.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labplatform.application.port.out.PaymentGatewayPort;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PaymentStatus;
import com.labplatform.domain.shared.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

/**
 * Paiement par carte, via les sessions de paiement hébergées de Stripe.
 * <p>
 * La carte n'est jamais saisie sur la plateforme : le payeur est envoyé chez le
 * prestataire, qui nous rend un identifiant de session. Aucune donnée de carte
 * ne traverse donc ce code, ce qui est ce qui rend l'intégration tenable.
 * <p>
 * Notre référence voyage dans {@code client_reference_id} et revient telle
 * quelle dans la notification : c'est ainsi qu'un encaissement retrouve le
 * paiement qu'il règle.
 */
public class StripeCheckoutGateway implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutGateway.class);
    private static final String DEFAULT_BASE_URL = "https://api.stripe.com";

    private final RestClient http;
    private final ObjectMapper json;
    private final String secretKey;
    private final String webhookSecret;
    private final Clock clock;

    public StripeCheckoutGateway(RestClient.Builder builder, ObjectMapper json, String baseUrl, String secretKey,
                                 String webhookSecret, Clock clock) {
        this.http = builder.baseUrl(baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl).build();
        this.json = json;
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.clock = clock;
    }

    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.CARD && secretKey != null && !secretKey.isBlank();
    }

    @Override
    public Checkout open(CheckoutRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("client_reference_id", request.reference());
        form.add("success_url", request.successUrl());
        form.add("cancel_url", request.cancelUrl());
        form.add("metadata[reference]", request.reference());
        form.add("line_items[0][quantity]", "1");
        form.add("line_items[0][price_data][currency]",
                request.amount().currencyCode().toLowerCase(Locale.ROOT));
        // Stripe attend le montant en unités indivisibles, ce qui est
        // exactement la façon dont Money le tient.
        form.add("line_items[0][price_data][unit_amount]", Long.toString(request.amount().minorUnits()));
        form.add("line_items[0][price_data][product_data][name]", request.description());

        JsonNode session = call(() -> http.post()
                .uri("/v1/checkout/sessions")
                .header("Authorization", "Bearer " + secretKey)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class));

        String id = text(session, "id");
        String url = text(session, "url");
        if (id == null || url == null) {
            throw new ServiceUnavailableException("Réponse inattendue du prestataire de paiement");
        }
        return new Checkout(url, id);
    }

    @Override
    public PaymentStatus verify(PaymentMethod method, String providerReference) {
        JsonNode session = call(() -> http.get()
                .uri("/v1/checkout/sessions/{id}", providerReference)
                .header("Authorization", "Bearer " + secretKey)
                .retrieve()
                .body(JsonNode.class));
        return statusOf(text(session, "status"), text(session, "payment_status"));
    }

    @Override
    public Optional<Notification> readNotification(PaymentMethod method, String signature, String rawBody) {
        SignedPayload.requireValid(signature, rawBody, webhookSecret, clock.instant());
        JsonNode event = parse(rawBody);
        String type = text(event, "type");
        JsonNode object = event.path("data").path("object");
        String reference = text(object, "client_reference_id");
        String providerReference = text(object, "id");
        if (type == null || reference == null) {
            return Optional.empty();
        }
        return switch (type) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" ->
                    Optional.of(new Notification(reference, providerReference,
                            statusOf("complete", text(object, "payment_status")), null));
            case "checkout.session.expired" ->
                    Optional.of(new Notification(reference, providerReference, PaymentStatus.CANCELLED, "Session expirée"));
            case "checkout.session.async_payment_failed" ->
                    Optional.of(new Notification(reference, providerReference, PaymentStatus.FAILED, "Paiement refusé"));
            // Stripe émet bien d'autres événements : les ignorer est normal.
            default -> Optional.empty();
        };
    }

    /**
     * Une session « complète » dont le paiement n'est pas encaissé reste en
     * attente : le virement bancaire différé, par exemple, se conclut plus tard.
     */
    private static PaymentStatus statusOf(String sessionStatus, String paymentStatus) {
        if ("paid".equals(paymentStatus) || "no_payment_required".equals(paymentStatus)) {
            return PaymentStatus.SUCCEEDED;
        }
        if ("expired".equals(sessionStatus)) {
            return PaymentStatus.CANCELLED;
        }
        return PaymentStatus.PENDING;
    }

    private JsonNode parse(String rawBody) {
        try {
            return json.readTree(rawBody);
        } catch (com.fasterxml.jackson.core.JsonProcessingException malformed) {
            throw new ServiceUnavailableException("Notification de paiement illisible");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private <T> T call(java.util.function.Supplier<T> exchange) {
        try {
            return exchange.get();
        } catch (RestClientException unreachable) {
            log.warn("Appel au prestataire de paiement en échec", unreachable);
            throw new ServiceUnavailableException("Le service de paiement ne répond pas");
        }
    }
}
