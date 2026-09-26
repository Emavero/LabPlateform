package com.labplatform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labplatform.adapter.out.payment.PaymentGatewayRegistry;
import com.labplatform.adapter.out.payment.SimulatedPaymentGateway;
import com.labplatform.adapter.out.payment.StripeCheckoutGateway;
import com.labplatform.adapter.out.payment.WaveCheckoutGateway;
import com.labplatform.application.port.out.PaymentGatewayPort;
import com.labplatform.application.service.BillingSettings;
import com.labplatform.domain.billing.Money;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PriceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Branchement des encaisseurs.
 * <p>
 * C'est ici, et seulement ici, que se décide qui encaisse : le service de
 * facturation ne voit qu'un {@link PaymentGatewayPort}. Ajouter un opérateur
 * consiste à écrire un adaptateur et à l'inscrire dans la liste ci-dessous.
 */
@Configuration
public class BillingConfig {

    private static final Logger log = LoggerFactory.getLogger(BillingConfig.class);

    @Bean
    public BillingSettings billingSettings(AppProperties properties) {
        AppProperties.Billing billing = properties.getBilling();
        PriceList defaults = priceList(billing.getCurrency(), billing.getMonthly(), billing.getYearly());

        Map<PaymentMethod, PriceList> overrides = new EnumMap<>(PaymentMethod.class);
        if (billing.getCard().isDefined()) {
            overrides.put(PaymentMethod.CARD, priceList(billing.getCard().getCurrency(),
                    billing.getCard().getMonthly(), billing.getCard().getYearly()));
        }
        return new BillingSettings(billing.isEnabled(), defaults, overrides, methods(billing.getMethods()),
                billing.getSuccessUrl(), billing.getCancelUrl());
    }

    @Bean
    public PaymentGatewayPort paymentGateway(AppProperties properties, RestClient.Builder restClients,
                                             ObjectMapper json, Clock clock) {
        AppProperties.Billing billing = properties.getBilling();
        if (!billing.isLive()) {
            if (billing.isEnabled()) {
                log.warn("Facturation en mode simulé : tout compte peut s'attribuer un abonnement Pro. "
                        + "Passez app.billing.mode à « live » sur une installation qui facture.");
            }
            return new SimulatedPaymentGateway(Set.of(PaymentMethod.values()));
        }

        List<PaymentGatewayPort> gateways = new ArrayList<>();
        gateways.add(new StripeCheckoutGateway(restClients, json, billing.getStripe().getBaseUrl(),
                billing.getStripe().getSecretKey(), billing.getStripe().getWebhookSecret(), clock));
        gateways.add(new WaveCheckoutGateway(restClients, json, billing.getWave().getBaseUrl(),
                billing.getWave().getApiKey(), billing.getWave().getWebhookSecret(), clock));

        // Un moyen déclaré mais sans clé reste annoncé comme indisponible, ce
        // que la page d'abonnement montre : mieux vaut un bouton grisé qu'un
        // parcours qui échoue au dernier écran.
        for (PaymentMethod method : methods(billing.getMethods())) {
            if (gateways.stream().noneMatch(gateway -> gateway.supports(method))) {
                log.warn("Moyen de paiement {} activé mais non configuré : il sera proposé comme indisponible",
                        method);
            }
        }
        return new PaymentGatewayRegistry(gateways);
    }

    private static PriceList priceList(String currency, java.math.BigDecimal monthly, java.math.BigDecimal yearly) {
        return new PriceList(Money.of(currency, monthly), Money.of(currency, yearly));
    }

    private static Set<PaymentMethod> methods(List<String> configured) {
        Set<PaymentMethod> methods = new LinkedHashSet<>();
        for (String raw : configured) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                methods.add(PaymentMethod.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException unknown) {
                log.warn("Moyen de paiement inconnu ignoré : {}", raw);
            }
        }
        return methods;
    }
}
