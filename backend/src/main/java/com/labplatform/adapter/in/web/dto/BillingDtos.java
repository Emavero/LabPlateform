package com.labplatform.adapter.in.web.dto;

import com.labplatform.adapter.in.web.Texts;
import com.labplatform.application.port.in.billing.BillingView;
import com.labplatform.application.port.in.billing.CheckoutTicket;
import com.labplatform.application.port.in.billing.PaymentSummary;
import com.labplatform.application.port.in.billing.PlanOffer;
import com.labplatform.domain.billing.BillingPeriod;
import com.labplatform.domain.billing.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Représentations HTTP de l'abonnement.
 * <p>
 * Les montants sortent sous deux formes : la valeur décimale, qui sert à
 * l'affichage, et les unités indivisibles, qui servent aux comparaisons. Le
 * client n'a ainsi jamais à savoir combien de décimales a une devise.
 */
public final class BillingDtos {

    private BillingDtos() {
    }

    public record MoneyResponse(long minorUnits, BigDecimal amount, String currency) {

        public static MoneyResponse from(com.labplatform.domain.billing.Money money) {
            return new MoneyResponse(money.minorUnits(), money.toDecimal(), money.currencyCode());
        }
    }

    public record OfferResponse(String method, String methodName, String period, String periodName,
                                MoneyResponse price, boolean available) {

        public static OfferResponse from(PlanOffer offer) {
            return new OfferResponse(offer.method().name(), Texts.of(offer.method().displayName()), offer.period().name(),
                    Texts.of(offer.period().displayName()), MoneyResponse.from(offer.price()), offer.available());
        }
    }

    public record PaymentResponse(String reference, MoneyResponse amount, String method, String methodName,
                                  String status, Instant createdAt, Instant settledAt) {

        public static PaymentResponse from(PaymentSummary summary) {
            return new PaymentResponse(summary.reference(), MoneyResponse.from(summary.amount()),
                    summary.method().name(), Texts.of(summary.method().displayName()), summary.status().name(),
                    summary.createdAt(), summary.settledAt());
        }
    }

    public record BillingResponse(String plan, String planName, String status, Instant expiresAt, boolean pro,
                                  boolean renewing, List<OfferResponse> offers, List<PaymentResponse> payments) {

        public static BillingResponse from(BillingView view) {
            return new BillingResponse(
                    view.plan().name(),
                    Texts.of(view.plan().displayName()),
                    view.status().name(),
                    view.expiresAt(),
                    view.plan().isPro(),
                    view.renewing(),
                    view.offers().stream().map(OfferResponse::from).toList(),
                    view.payments().stream().map(PaymentResponse::from).toList());
        }
    }

    public record CheckoutRequest(
            @NotNull(message = "Le moyen de paiement est obligatoire") PaymentMethod method,
            @NotNull(message = "La durée est obligatoire") BillingPeriod period) {
    }

    public record CheckoutResponse(String reference, String redirectUrl) {

        public static CheckoutResponse from(CheckoutTicket ticket) {
            return new CheckoutResponse(ticket.reference(), ticket.redirectUrl());
        }
    }
}
