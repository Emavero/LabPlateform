package com.labplatform.application.service;

import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.fakes.InMemoryCompletions;
import com.labplatform.application.fakes.InMemoryCourses;
import com.labplatform.application.fakes.InMemoryJournal;
import com.labplatform.application.fakes.InMemoryOwns;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.report.ActivityReport;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String USER_FLAG = "6f6e6d6c6b6a69686766656463626160";
    private static final String ROOT_FLAG = "7f7e7d7c7b7a79787776757473727170";

    private final InMemoryJournal journal = new InMemoryJournal();
    private final InMemoryOwns owns = new InMemoryOwns();
    private final InMemoryBoxes boxes = new InMemoryBoxes();
    private final InMemoryCourses courses = new InMemoryCourses();
    private final InMemoryCompletions completions = new InMemoryCompletions();

    private final Actor alice = new Actor(1L, Role.USER);
    private final Actor bob = new Actor(2L, Role.USER);
    private ReportService reports;
    private Long boxId;

    @BeforeEach
    void setUp() {
        reports = new ReportService(journal, owns, boxes, courses, completions, Clock.fixed(NOW, ZoneOffset.UTC));
        boxId = boxes.save(Box.create("sentinel", "Sentinel", OperatingSystem.LINUX, Difficulty.EASY, "Synopsis.",
                "10.10.10.11", "cyberMans", NOW.minus(Duration.ofDays(120)), Flag.ofSecret(USER_FLAG),
                Flag.ofSecret(ROOT_FLAG))).getId();
    }

    private void own(Actor actor, FlagKind kind, Instant when) {
        owns.save(new Own(null, actor.userId(), boxId, kind, kind == FlagKind.USER ? 8 : 12, false, when));
    }

    private void act(Actor actor, JournalKind kind, Instant when) {
        journal.record(JournalEvent.of(actor.userId(), kind, "sentinel", when));
    }

    @Test
    void leRapportCompteLesValidationsDeLaPeriode() {
        own(alice, FlagKind.USER, NOW.minus(Duration.ofDays(3)));
        own(alice, FlagKind.ROOT, NOW.minus(Duration.ofDays(2)));

        ActivityReport report = reports.report(alice, 30);

        assertEquals(2, report.totals().flags());
        assertEquals(20, report.totals().points());
        assertEquals(1, report.totals().boxesPwned());
    }

    /**
     * Une machine dont le flag root a été validé avant la période n'est pas
     * « possédée sur la période » : le rapport gonflerait d'un progrès qui n'a
     * pas eu lieu.
     */
    @Test
    void unePossessionACheValSurDeuxPeriodesNeCompteQueSesFlagsDeLaPeriode() {
        own(alice, FlagKind.USER, NOW.minus(Duration.ofDays(45)));
        own(alice, FlagKind.ROOT, NOW.minus(Duration.ofDays(2)));

        ActivityReport report = reports.report(alice, 30);

        assertEquals(1, report.totals().flags());
        assertEquals(0, report.totals().boxesPwned());
    }

    @Test
    void leRapportNeParleQueDuCompteQuiLeDemande() {
        own(bob, FlagKind.USER, NOW.minus(Duration.ofDays(1)));
        act(bob, JournalKind.BOX_VIEWED, NOW.minus(Duration.ofDays(1)));

        ActivityReport report = reports.report(alice, 30);

        assertEquals(0, report.totals().flags());
        assertTrue(report.totals().isIdle());
        assertTrue(report.machines().isEmpty());
    }

    @Test
    void laPeriodePrecedenteEstMesureeALIdentique() {
        own(alice, FlagKind.USER, NOW.minus(Duration.ofDays(2)));
        own(alice, FlagKind.USER, NOW.minus(Duration.ofDays(40)));

        ActivityReport report = reports.report(alice, 30);

        assertEquals(1, report.totals().flags());
        assertEquals(1, report.previous().flags());
    }

    @Test
    void lesJoursActifsSeComptentParJourDistinct() {
        act(alice, JournalKind.BOX_VIEWED, NOW.minus(Duration.ofDays(2)));
        act(alice, JournalKind.FLAG_REFUSED, NOW.minus(Duration.ofDays(2)).plus(Duration.ofHours(3)));
        act(alice, JournalKind.BOX_SPAWNED, NOW.minus(Duration.ofDays(5)));

        ActivityReport report = reports.report(alice, 30);

        assertEquals(3, report.totals().events());
        assertEquals(2, report.totals().activeDays());
    }

    /** Un rapport honnête montre aussi les échecs : ils disent où ça bloque. */
    @Test
    void leRapportMontreLesQuizManquesCommeLesReussis() {
        act(alice, JournalKind.QUIZ_PASSED, NOW.minus(Duration.ofDays(1)));
        act(alice, JournalKind.QUIZ_FAILED, NOW.minus(Duration.ofDays(1)));
        act(alice, JournalKind.QUIZ_FAILED, NOW.minus(Duration.ofDays(1)));

        ActivityReport report = reports.report(alice, 30);

        assertEquals(1, report.totals().quizzesPassed());
        assertEquals(2, report.totals().quizzesFailed());
        assertEquals(33, report.totals().quizSuccessPercent());
    }

    @Test
    void lesFamillesEtLesNaturesSontDecomptees() {
        act(alice, JournalKind.BOX_VIEWED, NOW.minus(Duration.ofDays(1)));
        act(alice, JournalKind.BOX_VIEWED, NOW.minus(Duration.ofDays(1)));
        act(alice, JournalKind.QUIZ_PASSED, NOW.minus(Duration.ofDays(1)));

        ActivityReport report = reports.report(alice, 30);

        assertEquals(2, report.families().size());
        assertEquals("MACHINES", report.families().get(0).code());
        assertEquals(2, report.families().get(0).count());
        assertEquals("BOX_VIEWED", report.kinds().get(0).code());
    }

    @Test
    void laMachinePossedeeApparaitAvecSonNomEtSesPoints() {
        own(alice, FlagKind.USER, NOW.minus(Duration.ofDays(3)));
        own(alice, FlagKind.ROOT, NOW.minus(Duration.ofDays(3)));

        ActivityReport.MachineLine line = reports.report(alice, 30).machines().get(0);

        assertEquals("sentinel", line.slug());
        assertEquals("Sentinel", line.name());
        assertEquals(2, line.flags());
        assertEquals(20, line.points());
        assertTrue(line.pwned());
        assertNotNull(line.lastAt());
    }

    @Test
    void laPeriodeEstRameneeDansSesBornes() {
        assertEquals(7, reports.report(alice, 1).days());
        assertEquals(365, reports.report(alice, 9_000).days());
    }

    @Test
    void unePeriodeSansActeSeLitCommeInactive() {
        ActivityReport report = reports.report(alice, 30);

        assertTrue(report.totals().isIdle());
        assertFalse(report.totals().quizSuccessPercent() >= 0);
        assertTrue(report.families().isEmpty());
        assertTrue(report.courses().isEmpty());
    }
}
