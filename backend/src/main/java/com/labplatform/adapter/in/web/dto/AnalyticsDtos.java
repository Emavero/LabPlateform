package com.labplatform.adapter.in.web.dto;

import com.labplatform.adapter.in.web.Texts;
import com.labplatform.application.port.in.analytics.AdminAnalytics;
import com.labplatform.application.port.in.analytics.AudienceMetrics;
import com.labplatform.application.port.in.analytics.ContentInsight;
import com.labplatform.application.port.in.analytics.EngagementMetrics;
import com.labplatform.application.port.in.analytics.RevenueMetrics;
import com.labplatform.application.port.in.analytics.UsageSegment;
import com.labplatform.application.port.in.analytics.WindowComparison;
import com.labplatform.domain.insight.Recommendation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Représentation HTTP des indicateurs d'administration.
 * <p>
 * Les libellés accompagnent les codes : le client affiche ce que le serveur
 * envoie, plutôt que d'entretenir une table de correspondance qui se
 * désynchroniserait à chaque profil ou règle ajoutés.
 */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record MetricResponse(String label, long value, String unit) {
    }

    public record SegmentResponse(String profile, String profileName, String advice, long accounts, int share) {

        static SegmentResponse from(UsageSegment segment) {
            return new SegmentResponse(segment.profile().name(), Texts.of(segment.profile().displayName()),
                    segment.profile().advice(), segment.accounts(), segment.share());
        }
    }

    public record InsightEntryResponse(String subject, long count) {
    }

    public record InsightResponse(String code, String title, String unit, List<InsightEntryResponse> entries) {

        static InsightResponse from(ContentInsight insight) {
            return new InsightResponse(insight.code(), insight.title(), insight.unit(),
                    insight.entries().stream()
                            .map(entry -> new InsightEntryResponse(entry.subject(), entry.count()))
                            .toList());
        }
    }

    public record RecommendationResponse(String code, String severity, String severityName, String title,
                                        String advice, String evidence, String subject) {

        static RecommendationResponse from(Recommendation recommendation) {
            return new RecommendationResponse(recommendation.code(), recommendation.severity().name(),
                    Texts.of(recommendation.severity().displayName()), recommendation.title(), recommendation.advice(),
                    recommendation.evidence(), recommendation.subject());
        }
    }

    public record RevenueResponse(List<BillingDtos.MoneyResponse> collected, long paymentsSucceeded,
                                  long paymentsFailed, int failureRate, long checkoutsStarted,
                                  int checkoutCompletionRate) {

        static RevenueResponse from(RevenueMetrics revenue) {
            return new RevenueResponse(revenue.collected().stream().map(BillingDtos.MoneyResponse::from).toList(),
                    revenue.paymentsSucceeded(), revenue.paymentsFailed(), revenue.failureRate(),
                    revenue.checkoutsStarted(), revenue.checkoutCompletionRate());
        }
    }

    public record AnalyticsResponse(Instant generatedAt, int windowDays, Map<String, Long> catalogue,
                                    AudienceMetrics audience, EngagementMetrics engagement, RevenueResponse revenue,
                                    WindowComparison previous, List<SegmentResponse> segments,
                                    List<InsightResponse> insights,
                                    List<RecommendationResponse> recommendations) {

        public static AnalyticsResponse from(AdminAnalytics analytics) {
            AudienceMetrics audience = analytics.audience();
            EngagementMetrics engagement = analytics.engagement();
            return new AnalyticsResponse(
                    analytics.generatedAt(),
                    analytics.windowDays(),
                    Map.of("boxes", analytics.boxesPublished(),
                            "courses", analytics.coursesPublished(),
                            "sections", analytics.sectionsPublished()),
                    audience,
                    engagement,
                    RevenueResponse.from(analytics.revenue()),
                    analytics.previous(),
                    analytics.segments().stream().map(SegmentResponse::from).toList(),
                    analytics.insights().stream().map(InsightResponse::from).toList(),
                    analytics.recommendations().stream().map(RecommendationResponse::from).toList());
        }
    }
}
