package com.labplatform.application.service;

import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.fakes.InMemoryCourses;
import com.labplatform.application.fakes.InMemoryJournal;
import com.labplatform.application.fakes.InMemoryPayments;
import com.labplatform.application.fakes.InMemorySubscriptions;
import com.labplatform.application.fakes.InMemoryUsers;
import com.labplatform.application.port.in.analytics.AdminAnalytics;
import com.labplatform.application.port.in.analytics.ContentInsight;
import com.labplatform.application.port.in.analytics.UsageSegment;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseTopic;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.academy.Track;
import com.labplatform.domain.billing.BillingPeriod;
import com.labplatform.domain.billing.Money;
import com.labplatform.domain.billing.Payment;
import com.labplatform.domain.billing.PaymentMethod;
import com.labplatform.domain.billing.Subscription;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.insight.UsageProfile;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.Role;
import com.labplatform.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAnalyticsServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");
    private static final Instant LONG_AGO = NOW.minusSeconds(120L * 86_400);
    private static final Actor ADMIN = new Actor(99L, Role.ADMIN);
    private static final Actor ALICE = new Actor(1L, Role.USER);

    private InMemoryUsers users;
    private InMemoryJournal journal;
    private InMemorySubscriptions subscriptions;
    private InMemoryPayments payments;
    private AdminAnalyticsService analytics;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        journal = new InMemoryJournal();
        subscriptions = new InMemorySubscriptions();
        payments = new InMemoryPayments();
        InMemoryBoxes boxes = new InMemoryBoxes();
        InMemoryCourses courses = new InMemoryCourses();

        // Cinq comptes et un administrateur.
        account(1L, "alice@example.com", NOW.minusSeconds(40L * 86_400));
        account(2L, "bob@example.com", NOW.minusSeconds(40L * 86_400));
        account(3L, "carol@example.com", NOW.minusSeconds(40L * 86_400));
        account(4L, "dan@example.com", NOW.minusSeconds(40L * 86_400));
        account(5L, "erin@example.com", NOW.minusSeconds(2L * 86_400));
        users.save(User.restore(99L, Email.of("admin@example.com"), "hash", Role.ADMIN, LONG_AGO, null));

        boxes.save(Box.create("sentinel", "Sentinel", OperatingSystem.LINUX, Difficulty.EASY, "S.", "10.0.0.1",
                "cyberMans", NOW, Flag.ofSecret("a".repeat(32)), Flag.ofSecret("b".repeat(32))));
        courses.save(course("traces", Track.FORENSICS));
        courses.save(course("durcir", Track.DEFENSE));

        analytics = new AdminAnalyticsService(users, boxes, courses, subscriptions, payments, journal,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void account(long id, String email, Instant createdAt) {
        users.save(User.restore(id, Email.of(email), "hash", Role.USER, createdAt, null));
    }

    private static Course course(String slug, Track track) {
        List<CourseSection> sections = List.of(
                CourseSection.of(null, "s1", "Section 1", SectionKind.THEORY, 1, 10, "Contenu"));
        return Course.create(slug, slug, CourseTopic.of(track).get(0), CourseLevel.FUNDAMENTAL, "Résumé.", LONG_AGO,
                sections);
    }

    private void record(long userId, JournalKind kind, String subject, Instant at) {
        journal.record(JournalEvent.of(userId, kind, subject, at));
    }

    @Test
    void onlyTheAdministratorReadsTheIndicators() {
        assertThrows(ForbiddenException.class, () -> analytics.analytics(ALICE, 30));
    }

    /**
     * L'administration n'est pas sa propre audience : la compter gonflerait
     * l'activité et fausserait le taux de conversion.
     */
    @Test
    void theAdministratorIsNotCountedAmongTheAccounts() {
        assertEquals(5, analytics.analytics(ADMIN, 30).audience().users());
    }

    /** Tout se rapporte à la fenêtre : un vieux succès ne doit pas flatter le mois. */
    @Test
    void onlyCountsWhatHappenedInsideTheWindow() {
        record(1L, JournalKind.FLAG_VALIDATED, "sentinel", NOW.minusSeconds(3600));
        record(2L, JournalKind.FLAG_VALIDATED, "sentinel", LONG_AGO);

        assertEquals(1, analytics.analytics(ADMIN, 30).engagement().flagsValidated());
    }

    /** Le cœur de la demande : classer les comptes par ce dont ils se servent. */
    @Test
    void classifiesAccountsByWhatTheyActuallyUse() {
        record(1L, JournalKind.FLAG_VALIDATED, "sentinel", NOW.minusSeconds(3600));
        record(2L, JournalKind.SECTION_COMPLETED, "traces", NOW.minusSeconds(3600));
        record(3L, JournalKind.VPN_PROFILE_ISSUED, null, NOW.minusSeconds(3600));
        // Carol regarde sans agir : c'est une curieuse, pas une joueuse.
        record(4L, JournalKind.BOX_VIEWED, "sentinel", NOW.minusSeconds(3600));
        // Erin n'a rien fait depuis son inscription.

        List<UsageSegment> segments = analytics.analytics(ADMIN, 30).segments();

        assertEquals(1, shareOf(segments, UsageProfile.HUNTER));
        assertEquals(1, shareOf(segments, UsageProfile.LEARNER));
        assertEquals(1, shareOf(segments, UsageProfile.BUILDER));
        assertEquals(1, shareOf(segments, UsageProfile.BROWSER));
        assertEquals(1, shareOf(segments, UsageProfile.DORMANT));
    }

    @Test
    void countsAsActiveWhoeverActed() {
        record(1L, JournalKind.FLAG_VALIDATED, "sentinel", NOW.minusSeconds(3600));
        record(2L, JournalKind.BOX_VIEWED, "sentinel", NOW.minusSeconds(3600));

        var audience = analytics.analytics(ADMIN, 30).audience();

        // Regarder compte comme une activité, même si ce n'est pas un acte.
        assertEquals(2, audience.activeUsers());
        assertEquals(3, audience.dormantUsers());
        assertEquals(40, audience.activeRate());
    }

    @Test
    void newcomersAreTheAccountsOpenedInsideTheWindow() {
        assertEquals(1, analytics.analytics(ADMIN, 30).audience().newUsers());
    }

    @Test
    void conversionIsTheShareOfSubscribedAccounts() {
        Subscription pro = Subscription.free(1L);
        pro.extend(BillingPeriod.MONTHLY, NOW.minusSeconds(86_400));
        subscriptions.save(pro);

        var audience = analytics.analytics(ADMIN, 30).audience();

        assertEquals(1, audience.proUsers());
        assertEquals(20, audience.conversionRate());
    }

    /** Additionner des francs CFA et des euros donnerait un nombre qui ne veut rien dire. */
    @Test
    void revenueIsTotalledPerCurrency() {
        settle("1".repeat(32), 1L, Money.of("XOF", new BigDecimal("5000")), PaymentMethod.WAVE);
        settle("2".repeat(32), 2L, Money.of("XOF", new BigDecimal("5000")), PaymentMethod.WAVE);
        settle("3".repeat(32), 3L, Money.of("EUR", new BigDecimal("8.00")), PaymentMethod.CARD);

        var revenue = analytics.analytics(ADMIN, 30).revenue();

        assertEquals(2, revenue.collected().size());
        assertEquals(10_000L, revenue.collected().stream()
                .filter(money -> money.currencyCode().equals("XOF"))
                .findFirst().orElseThrow().minorUnits());
        assertEquals(3, revenue.paymentsSucceeded());
    }

    @Test
    void ranksContentByWhatAttractsAndWhatBlocks() {
        record(1L, JournalKind.BOX_VIEWED, "sentinel", NOW.minusSeconds(60));
        record(2L, JournalKind.BOX_VIEWED, "sentinel", NOW.minusSeconds(60));
        record(3L, JournalKind.BOX_VIEWED, "obsidian", NOW.minusSeconds(60));
        record(1L, JournalKind.FLAG_REFUSED, "obsidian", NOW.minusSeconds(60));

        List<ContentInsight> insights = analytics.analytics(ADMIN, 30).insights();

        ContentInsight viewed = insight(insights, "viewed-boxes");
        assertEquals("sentinel", viewed.entries().get(0).subject());
        assertEquals(2, viewed.entries().get(0).count());
        assertEquals("obsidian", insight(insights, "refused-flags").entries().get(0).subject());
    }

    /** Les recommandations viennent des mêmes chiffres, et portent leur preuve. */
    @Test
    void turnsAPaywallHitIntoARecommendation() {
        for (int i = 0; i < 8; i++) {
            record(1L, JournalKind.BOX_LOCKED_OUT, "blackice", NOW.minusSeconds(60L * (i + 1)));
        }

        AdminAnalytics report = analytics.analytics(ADMIN, 30);

        assertTrue(report.recommendations().stream()
                .anyMatch(recommendation -> recommendation.code().equals("locked-out-demand")
                        && "blackice".equals(recommendation.subject())));
    }

    @Test
    void reportsTheCatalogueSize() {
        AdminAnalytics report = analytics.analytics(ADMIN, 30);

        assertEquals(1, report.boxesPublished());
        assertEquals(2, report.coursesPublished());
        assertEquals(2, report.sectionsPublished());
        assertEquals(30, report.windowDays());
    }

    /** Une fenêtre absurde ne doit pas produire de chiffres absurdes. */
    @Test
    void aWindowIsAlwaysBoundedToSomethingSensible() {
        assertEquals(30, analytics.analytics(ADMIN, 0).windowDays());
        assertEquals(365, analytics.analytics(ADMIN, 100_000).windowDays());
    }

    private void settle(String reference, long userId, Money amount, PaymentMethod method) {
        Payment payment = Payment.initiate(reference, userId, BillingPeriod.MONTHLY, amount, method,
                NOW.minusSeconds(7200));
        payment.succeed(NOW.minusSeconds(3600));
        payments.save(payment);
    }

    private static long shareOf(List<UsageSegment> segments, UsageProfile profile) {
        return segments.stream()
                .filter(segment -> segment.profile() == profile)
                .findFirst()
                .orElseThrow()
                .accounts();
    }

    private static ContentInsight insight(List<ContentInsight> insights, String code) {
        return insights.stream()
                .filter(insight -> insight.code().equals(code))
                .findFirst()
                .orElseThrow();
    }

    /** Un chiffre sans comparaison ne dit rien : la période précédente est fournie. */
    @Test
    void comparesTheWindowWithThePreviousOne() {
        record(1L, JournalKind.FLAG_VALIDATED, "sentinel", NOW.minusSeconds(3600));
        record(2L, JournalKind.FLAG_VALIDATED, "sentinel", NOW.minusSeconds(3600));
        // Il y a 45 jours : dans la période précédente d'une fenêtre de 30 jours.
        record(3L, JournalKind.FLAG_VALIDATED, "sentinel", NOW.minusSeconds(45L * 86_400));
        record(4L, JournalKind.SECTION_COMPLETED, "traces", NOW.minusSeconds(45L * 86_400));

        AdminAnalytics report = analytics.analytics(ADMIN, 30);

        assertEquals(2, report.engagement().flagsValidated());
        assertEquals(1, report.previous().flagsValidated());
        assertEquals(1, report.previous().sectionsCompleted());
        // Deux comptes distincts ont agi sur la période précédente.
        assertEquals(2, report.previous().activeUsers());
    }
}
