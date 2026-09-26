package com.labplatform.application.service;

import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.PriceList;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Réglages de facturation, lus une fois au démarrage et passés au service :
 * celui-ci ne connaît donc ni fichier de configuration ni variable
 * d'environnement.
 *
 * @param overrides tarif propre à un moyen de paiement. Sans cette exception,
 *                  on facturerait la carte en francs CFA, ce que les réseaux
 *                  bancaires n'acceptent pas, ou Wave en euros, ce que
 *                  l'opérateur refuse.
 */
public record BillingSettings(boolean enabled, PriceList defaultPrices, Map<PaymentMethod, PriceList> overrides,
                              Set<PaymentMethod> enabledMethods, String successUrl, String cancelUrl) {

    public BillingSettings {
        Objects.requireNonNull(defaultPrices, "defaultPrices");
        overrides = Map.copyOf(new EnumMap<>(Objects.requireNonNull(overrides, "overrides")));
        enabledMethods = Set.copyOf(Objects.requireNonNull(enabledMethods, "enabledMethods"));
        Objects.requireNonNull(successUrl, "successUrl");
        Objects.requireNonNull(cancelUrl, "cancelUrl");
    }

    public PriceList pricesFor(PaymentMethod method) {
        return overrides.getOrDefault(method, defaultPrices);
    }

    public boolean isEnabled(PaymentMethod method) {
        return enabled && enabledMethods.contains(method);
    }
}
