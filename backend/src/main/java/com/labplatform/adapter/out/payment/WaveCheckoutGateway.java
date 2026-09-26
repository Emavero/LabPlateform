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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Paiement par portefeuille mobile Wave, très répandu en Afrique de l'Ouest.
 * <p>
 * Pourquoi ce second adaptateur plutôt que la seule carte : dans la zone où
 * la plateforme est utilisée, la carte bancaire est minoritaire, et un
 * abonnement qui ne s'achète qu'avec elle exclut la majorité des comptes. Le
 * parcours est le même que pour la carte — le payeur est envoyé chez
 * l'opérateur, qui nous notifie — seuls l'appel et la devise changent.
 * <p>
 * Le montant est envoyé tel qu'il s'écrit ({@code "5000"} pour 5 000 F CFA) :
 * le XOF n'a pas de subdivision, ce que {@code Money} sait déjà par la devise.
 */
public class WaveCheckoutGateway implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(WaveCheckoutGateway.class);
    private static final String DEFAULT_BASE_URL = "https://api.wave.com";

    private final RestClient http;
    private final ObjectMapper json;
    private final String apiKey;
    private final String webhookSecret;
    private final Clock clock;

    public WaveCheckoutGateway(RestClient.Builder builder, ObjectMapper json, String baseUrl, String apiKey,
                               String webhookSecret, Clock clock) {
        this.http = builder.baseUrl(baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl).build();
        this.json = json;
        this.apiKey = apiKey;
        this.webhookSecret = webhookSecret;
        this.clock = clock;
    }

    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.WAVE && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Checkout open(CheckoutRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", request.amount().toDecimal().toPlainString());
        body.put("currency", request.amount().currencyCode());
        body.put("success_url", request.successUrl());
        body.put("error_url", request.cancelUrl());
        body.put("client_reference", request.reference());

        JsonNode session = call(() -> http.post()
                .uri("/v1/checkout/sessions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class));

        String id = text(session, "id");
        String url = text(session, "wave_launch_url");
        if (id == null || url == null) {
            throw new ServiceUnavailableException("Réponse inattendue de l'opérateur de paiement");
        }
        return new Checkout(url, id);
    }

    @Override
    public PaymentStatus verify(PaymentMethod method, String providerReference) {
        JsonNode session = call(() -> http.get()
                .uri("/v1/checkout/sessions/{id}", providerReference)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .body(JsonNode.class));
        return statusOf(text(session, "checkout_status"), text(session, "payment_status"));
    }

    @Override
    public Optional<Notification> readNotification(PaymentMethod method, String signature, String rawBody) {
        SignedPayload.requireValid(signature, rawBody, webhookSecret, clock.instant());
        JsonNode event = parse(rawBody);
        JsonNode session = event.path("data");
        String reference = text(session, "client_reference");
        String providerReference = text(session, "id");
        if (reference == null) {
            return Optional.empty();
        }
        PaymentStatus status = statusOf(text(session, "checkout_status"), text(session, "payment_status"));
        if (status == PaymentStatus.PENDING) {
            return Optional.empty();
        }
        return Optional.of(new Notification(reference, providerReference, status,
                status == PaymentStatus.FAILED ? "Paiement refusé par l'opérateur" : null));
    }

    private static PaymentStatus statusOf(String checkoutStatus, String paymentStatus) {
        if ("succeeded".equals(paymentStatus)) {
            return PaymentStatus.SUCCEEDED;
        }
        if ("cancelled".equals(paymentStatus) || "expired".equals(checkoutStatus)) {
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
            log.warn("Appel à l'opérateur de paiement en échec", unreachable);
            throw new ServiceUnavailableException("Le service de paiement ne répond pas");
        }
    }
}
