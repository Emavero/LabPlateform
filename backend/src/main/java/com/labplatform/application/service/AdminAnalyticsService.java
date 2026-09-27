package com.labplatform.application.service;

import com.labplatform.application.port.in.analytics.AdminAnalytics;
import com.labplatform.application.port.in.analytics.AudienceMetrics;
import com.labplatform.application.port.in.analytics.ContentInsight;
import com.labplatform.application.port.in.analytics.EngagementMetrics;
import com.labplatform.application.port.in.analytics.GetAdminAnalyticsUseCase;
import com.labplatform.application.port.in.analytics.RevenueMetrics;
import com.labplatform.application.port.in.analytics.UsageSegment;
import com.labplatform.application.port.in.analytics.WindowComparison;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.PaymentRepositoryPort;
import com.labplatform.application.port.out.SubscriptionRepositoryPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.billing.Money;
import com.labplatform.domain.billing.Payment;
import com.labplatform.domain.insight.Recommendation;
import com.labplatform.domain.insight.RecommendationEngine;
import com.labplatform.domain.insight.Signals;
import com.labplatform.domain.insight.UsageProfile;
import com.labplatform.domain.journal.JournalFamily;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.AdminPolicy;
import com.labplatform.domain.user.Role;
import com.labplatform.domain.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Indicateurs d'administration.
 * <p>
 * Le service ne fait que deux choses : rassembler des décomptes, et classer les
 * comptes par ce dont ils se servent. Le jugement — « que faudrait-il faire ? »
 * — est confié au domaine ({@link RecommendationEngine}), qui est une fonction
 * pure : mêmes chiffres, mêmes conseils, éprouvables sans base de données.
 * <p>
 * Tous les chiffres portent sur une <em>fenêtre</em>, jamais sur l'histoire
 * complète : une plateforme d'un an dont personne ne se sert depuis six mois
 * afficherait sinon des totaux flatteurs et faux.
 */
public class AdminAnalyticsService implements GetAdminAnalyticsUseCase {

    private static final int DEFAULT_WINDOW_DAYS = 30;
    private static final int MAX_WINDOW_DAYS = 365;
    /** Au-delà, un classement ne se lit plus : on veut les têtes de liste. */
    private static final int TOP = 5;

    private final UserRepositoryPort users;
    private final BoxRepositoryPort boxes;
    private final CourseRepositoryPort courses;
    private final SubscriptionRepositoryPort subscriptions;
    private final PaymentRepositoryPort payments;
    private final JournalPort journal;
    private final Clock clock;

    public AdminAnalyticsService(UserRepositoryPort users, BoxRepositoryPort boxes, CourseRepositoryPort courses,
                                SubscriptionRepositoryPort subscriptions, PaymentRepositoryPort payments,
                                JournalPort journal, Clock clock) {
        this.users = users;
        this.boxes = boxes;
        this.courses = courses;
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.journal = journal;
        this.clock = clock;
    }

    @Override
    public AdminAnalytics analytics(Actor actor, int windowDays) {
        AdminPolicy.requireAdmin(actor);
        int window = windowDays <= 0 ? DEFAULT_WINDOW_DAYS : Math.min(windowDays, MAX_WINDOW_DAYS);
        Instant now = clock.instant();
        Instant since = now.minus(Duration.ofDays(window));
        // Période précédente, de même longueur : c'est elle qui donne la tendance.
        Instant before = since.minus(Duration.ofDays(window));

        // Un seul passage sur le journal de la fenêtre, dépouillé en base.
        Map<JournalKind, Long> counts = new EnumMap<>(JournalKind.class);
        journal.tallyByKindBetween(since, now).forEach(tally -> counts.put(tally.key(), tally.count()));

        List<User> accounts = users.findAll().stream()
                // L'administration n'est pas sa propre audience : la compter
                // gonflerait l'activité et fausserait la conversion.
                .filter(user -> user.getRole() != Role.ADMIN)
                .toList();
        Map<Long, UsageProfile> profiles = profilesOf(accounts, since);

        List<Course> catalogue = courses.findAll();
        long sections = catalogue.stream().mapToLong(course -> course.getSections().size()).sum();

        AudienceMetrics audience = audience(accounts, profiles, since, now);
        EngagementMetrics engagement = engagement(counts);
        RevenueMetrics revenue = revenue(counts, since, now);
        List<UsageSegment> segments = segments(profiles, accounts.size());
        List<ContentInsight> insights = insights(since, now);
        WindowComparison previous = previous(accounts, before, since);

        Signals signals = Signals.builder()
                .users(accounts.size())
                .activeUsers(audience.activeUsers())
                .proUsers(audience.proUsers())
                .checkoutsStarted(revenue.checkoutsStarted())
                .subscriptionsStarted(count(counts, JournalKind.SUBSCRIPTION_STARTED))
                .paymentsFailed(revenue.paymentsFailed())
                .flagsValidated(engagement.flagsValidated())
                .flagsRefused(engagement.flagsRefused())
                .quizPassed(engagement.quizPassed())
                .quizFailed(engagement.quizFailed())
                .targetsSpawned(engagement.targetsSpawned())
                .boxesPublished(boxes.count())
                .coursesPublished(catalogue.size())
                .lockedOutBoxes(entriesOf(insights, "locked-out"))
                .stuckBoxes(entriesOf(insights, "refused-flags"))
                .failedQuizzes(entriesOf(insights, "failed-quizzes"))
                .viewedCourses(entriesOf(insights, "viewed-courses"))
                .trackViews(trackViews(since, now))
                .profiles(byProfile(profiles))
                .build();
        List<Recommendation> recommendations = RecommendationEngine.from(signals);

        return new AdminAnalytics(now, window, boxes.count(), catalogue.size(), sections, audience, engagement,
                revenue, previous, segments, insights, recommendations);
    }

    /**
     * Profil de chaque compte, d'après la famille où il a le plus agi. Les
     * consultations ne comptent pas comme des actes : un compte qui ne fait que
     * regarder est un curieux, pas un joueur.
     */
    /**
     * Les mêmes chiffres sur la période précédente. Seuls ceux qui mènent la
     * lecture sont recalculés : une comparaison sur trente indicateurs coûterait
     * deux fois le tableau de bord pour rien.
     */
    private WindowComparison previous(List<User> accounts, Instant from, Instant to) {
        Map<JournalKind, Long> counts = new EnumMap<>(JournalKind.class);
        journal.tallyByKindBetween(from, to).forEach(tally -> counts.put(tally.key(), tally.count()));
        long active = journal.activityByUserBetween(from, to).stream()
                .filter(row -> row.kind().isEngagement())
                .map(JournalPort.UserActivity::userId)
                .distinct()
                .count();
        long newcomers = accounts.stream()
                .filter(user -> user.getCreatedAt().isAfter(from) && !user.getCreatedAt().isAfter(to))
                .count();
        return new WindowComparison(newcomers, active, count(counts, JournalKind.FLAG_VALIDATED),
                count(counts, JournalKind.SECTION_COMPLETED), count(counts, JournalKind.SUBSCRIPTION_STARTED));
    }

    private Map<Long, UsageProfile> profilesOf(List<User> accounts, Instant since) {
        Map<Long, Map<JournalFamily, Long>> actsByUser = new HashMap<>();
        Set<Long> seenOnly = new HashSet<>();
        for (JournalPort.UserActivity row : journal.activityByUserBetween(since, clock.instant())) {
            if (!row.kind().isEngagement()) {
                continue;
            }
            if (isPassive(row.kind())) {
                seenOnly.add(row.userId());
                continue;
            }
            actsByUser.computeIfAbsent(row.userId(), id -> new EnumMap<>(JournalFamily.class))
                    .merge(row.kind().family(), row.count(), Long::sum);
        }

        Map<Long, UsageProfile> profiles = new LinkedHashMap<>();
        for (User account : accounts) {
            Map<JournalFamily, Long> acts = actsByUser.get(account.getId());
            if (acts != null && !acts.isEmpty()) {
                JournalFamily dominant = acts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .orElseThrow()
                        .getKey();
                profiles.put(account.getId(), UsageProfile.of(dominant));
            } else if (seenOnly.contains(account.getId())) {
                profiles.put(account.getId(), UsageProfile.BROWSER);
            } else {
                profiles.put(account.getId(), UsageProfile.DORMANT);
            }
        }
        return profiles;
    }

    /** Regarder n'est pas agir : ces natures ne suffisent pas à définir un profil. */
    private static boolean isPassive(JournalKind kind) {
        return kind == JournalKind.BOX_VIEWED || kind == JournalKind.COURSE_VIEWED
                || kind == JournalKind.BOX_LOCKED_OUT;
    }

    private AudienceMetrics audience(List<User> accounts, Map<Long, UsageProfile> profiles, Instant since,
                                    Instant now) {
        long total = accounts.size();
        long newcomers = accounts.stream().filter(user -> user.getCreatedAt().isAfter(since)).count();
        long active = profiles.values().stream().filter(profile -> profile != UsageProfile.DORMANT).count();
        long dormant = total - active;
        long pro = subscriptions.findActiveAt(now).size();

        // Rétention : parmi les comptes qui existaient déjà au début de la
        // fenêtre, combien agissaient encore pendant celle-ci. Les nouveaux
        // venus en sont exclus — ils n'ont pas eu le temps de revenir.
        List<User> older = accounts.stream().filter(user -> !user.getCreatedAt().isAfter(since)).toList();
        long retained = older.stream()
                .filter(user -> profiles.get(user.getId()) != UsageProfile.DORMANT)
                .count();

        return new AudienceMetrics(total, newcomers, active, dormant, pro,
                percent(active, total), percent(pro, total), older.size(), percent(retained, older.size()));
    }

    private EngagementMetrics engagement(Map<JournalKind, Long> counts) {
        long validated = count(counts, JournalKind.FLAG_VALIDATED);
        long refused = count(counts, JournalKind.FLAG_REFUSED);
        long passed = count(counts, JournalKind.QUIZ_PASSED);
        long failed = count(counts, JournalKind.QUIZ_FAILED);
        return new EngagementMetrics(validated, refused,
                count(counts, JournalKind.BOX_PWNED),
                count(counts, JournalKind.BOX_SPAWNED),
                count(counts, JournalKind.COURSE_VIEWED),
                count(counts, JournalKind.SECTION_COMPLETED),
                passed, failed,
                count(counts, JournalKind.WRITEUP_PUBLISHED),
                percent(refused, validated + refused),
                percent(passed, passed + failed));
    }

    /** Un total par devise : additionner des euros et des francs CFA ne veut rien dire. */
    private RevenueMetrics revenue(Map<JournalKind, Long> counts, Instant since, Instant now) {
        Map<String, Money> byCurrency = new LinkedHashMap<>();
        List<Payment> collected = payments.findSucceededSince(since);
        for (Payment payment : collected) {
            byCurrency.merge(payment.getAmount().currencyCode(), payment.getAmount(), Money::plus);
        }
        long started = count(counts, JournalKind.CHECKOUT_STARTED);
        long succeeded = collected.size();
        long failed = count(counts, JournalKind.PAYMENT_FAILED);
        return new RevenueMetrics(List.copyOf(byCurrency.values()), succeeded, failed,
                percent(failed, succeeded + failed), started, percent(succeeded, started));
    }

    private List<UsageSegment> segments(Map<Long, UsageProfile> profiles, long total) {
        Map<UsageProfile, Long> byProfile = byProfile(profiles);
        List<UsageSegment> segments = new ArrayList<>();
        for (UsageProfile profile : UsageProfile.values()) {
            long accounts = byProfile.getOrDefault(profile, 0L);
            segments.add(new UsageSegment(profile, accounts, percent(accounts, total)));
        }
        segments.sort(Comparator.comparingLong(UsageSegment::accounts).reversed());
        return segments;
    }

    private static Map<UsageProfile, Long> byProfile(Map<Long, UsageProfile> profiles) {
        Map<UsageProfile, Long> counts = new EnumMap<>(UsageProfile.class);
        profiles.values().forEach(profile -> counts.merge(profile, 1L, Long::sum));
        return counts;
    }

    /**
     * Les classements qui disent ce qui attire et ce qui bloque. Chacun porte un
     * code stable : c'est lui que l'interface et les traductions utilisent, pas
     * le titre.
     */
    private List<ContentInsight> insights(Instant since, Instant now) {
        return List.of(
                insight("viewed-boxes", "Machines les plus consultées", "consultations",
                        JournalKind.BOX_VIEWED, since, now),
                insight("viewed-courses", "Cours les plus consultés", "consultations",
                        JournalKind.COURSE_VIEWED, since, now),
                insight("refused-flags", "Machines où les flags sont le plus refusés", "refus",
                        JournalKind.FLAG_REFUSED, since, now),
                insight("failed-quizzes", "Quiz les plus manqués", "échecs",
                        JournalKind.QUIZ_FAILED, since, now),
                insight("locked-out", "Machines réservées les plus convoitées", "tentatives sans abonnement",
                        JournalKind.BOX_LOCKED_OUT, since, now),
                insight("pwned-boxes", "Machines les plus possédées", "possessions",
                        JournalKind.BOX_PWNED, since, now));
    }

    private ContentInsight insight(String code, String title, String unit, JournalKind kind, Instant since,
                                   Instant now) {
        List<ContentInsight.Entry> entries = journal.tallyBySubjectBetween(kind, since, now, TOP).stream()
                .map(tally -> new ContentInsight.Entry(tally.key(), tally.count()))
                .toList();
        return new ContentInsight(code, title, unit, entries);
    }

    /**
     * Consultations par filière. Le détail de l'événement porte la filière du
     * cours consulté, ce qui évite de relire le catalogue pour chaque ligne.
     */
    private Map<String, Long> trackViews(Instant since, Instant now) {
        Map<String, Long> byTrack = new LinkedHashMap<>();
        for (Course course : courses.findAll()) {
            byTrack.putIfAbsent(course.getTrack().displayName(), 0L);
        }
        Map<String, String> trackOfCourse = new HashMap<>();
        courses.findAll().forEach(course -> trackOfCourse.put(course.getSlug(), course.getTrack().displayName()));
        for (JournalPort.Tally<String> tally
                : journal.tallyBySubjectBetween(JournalKind.COURSE_VIEWED, since, now, 100)) {
            String track = trackOfCourse.get(tally.key());
            if (track != null) {
                byTrack.merge(track, tally.count(), Long::sum);
            }
        }
        return byTrack;
    }

    private static List<Signals.Count> entriesOf(List<ContentInsight> insights, String code) {
        return insights.stream()
                .filter(insight -> insight.code().equals(code))
                .findFirst()
                .map(insight -> insight.entries().stream()
                        .map(entry -> new Signals.Count(entry.subject(), entry.count()))
                        .toList())
                .orElse(List.of());
    }

    private static long count(Map<JournalKind, Long> counts, JournalKind kind) {
        return counts.getOrDefault(kind, 0L);
    }

    /** Pourcentage entier, et zéro plutôt qu'une division par zéro. */
    private static int percent(long part, long total) {
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }
}
