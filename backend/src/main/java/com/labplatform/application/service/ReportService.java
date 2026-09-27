package com.labplatform.application.service;

import com.labplatform.application.port.in.report.GetActivityReportUseCase;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.application.port.out.SectionCompletionRepositoryPort;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.SectionCompletion;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalFamily;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.report.ActivityReport;
import com.labplatform.domain.report.ReportTally;
import com.labplatform.domain.report.ReportTotals;
import com.labplatform.domain.report.ReportWindow;
import com.labplatform.domain.user.Actor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rapports d'activité.
 * <p>
 * Le rapport se calcule à la demande et ne se stocke pas : un rapport
 * enregistré vieillirait mal — les cours sont réécrits, les machines retirées —
 * et il faudrait le régénérer pour s'y fier.
 * <p>
 * Les décomptes viennent de deux sources qui ne disent pas la même chose. Le
 * journal dit ce que le compte a <em>fait</em>, y compris ce qui a échoué ; les
 * validations et les sections terminées disent ce qu'il a <em>obtenu</em>. Le
 * rapport montre les deux : un mois à trente flags refusés et deux validés
 * raconte quelque chose qu'aucun des deux chiffres ne dit seul.
 */
public class ReportService implements GetActivityReportUseCase {

    /** Natures d'actes détaillées : au-delà, une liste ne se lit plus. */
    private static final int KIND_LIMIT = 8;

    private final JournalPort journal;
    private final OwnRepositoryPort owns;
    private final BoxRepositoryPort boxes;
    private final CourseRepositoryPort courses;
    private final SectionCompletionRepositoryPort completions;
    private final Clock clock;

    public ReportService(JournalPort journal, OwnRepositoryPort owns, BoxRepositoryPort boxes,
                         CourseRepositoryPort courses, SectionCompletionRepositoryPort completions, Clock clock) {
        this.journal = journal;
        this.owns = owns;
        this.boxes = boxes;
        this.courses = courses;
        this.completions = completions;
        this.clock = clock;
    }

    @Override
    public ActivityReport report(Actor actor, int days) {
        ReportWindow window = ReportWindow.lastDays(clock.instant(), days);
        ReportWindow before = window.previous();

        List<Own> allOwns = owns.findByUser(actor.userId());
        List<SectionCompletion> allCompletions = completions.findByUser(actor.userId());
        Map<Long, Box> boxesById = new HashMap<>();
        boxes.findAll().forEach(box -> boxesById.put(box.getId(), box));
        List<Course> catalogue = courses.findAll();

        ReportTotals totals = totals(actor, window, allOwns, allCompletions, catalogue);
        ReportTotals previous = totals(actor, before, allOwns, allCompletions, catalogue);
        List<JournalEvent> events = journal.findByUserBetween(actor.userId(), window.from(), window.to());

        return new ActivityReport(window.from(), window.to(), window.days(), totals, previous,
                families(events), kinds(events), machines(window, allOwns, boxesById),
                courseLines(window, allCompletions, catalogue));
    }

    private ReportTotals totals(Actor actor, ReportWindow window, List<Own> allOwns,
                                List<SectionCompletion> allCompletions, List<Course> catalogue) {
        List<JournalEvent> events = journal.findByUserBetween(actor.userId(), window.from(), window.to());
        List<Own> inWindow = allOwns.stream().filter(own -> window.contains(own.ownedAt())).toList();

        // Possédée « sur la période » demande les deux flags sur la période :
        // compter une machine dont le flag root avait été validé le mois
        // précédent gonflerait le rapport d'un progrès qui n'a pas eu lieu.
        Map<Long, Set<FlagKind>> flagsPerBox = new HashMap<>();
        inWindow.forEach(own -> flagsPerBox.computeIfAbsent(own.boxId(), key -> new HashSet<>()).add(own.kind()));
        long pwned = flagsPerBox.values().stream().filter(kinds -> kinds.size() == 2).count();

        Map<Long, CourseSection> sectionsById = sectionsById(catalogue);
        List<SectionCompletion> doneSections = allCompletions.stream()
                .filter(completion -> window.contains(completion.completedAt()))
                .toList();
        long minutes = doneSections.stream()
                .map(completion -> sectionsById.get(completion.sectionId()))
                .filter(java.util.Objects::nonNull)
                .mapToLong(CourseSection::minutes)
                .sum();

        long activeDays = events.stream()
                .map(event -> event.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate())
                .distinct()
                .count();

        return new ReportTotals(
                inWindow.size(),
                pwned,
                inWindow.stream().mapToLong(Own::points).sum(),
                doneSections.size(),
                count(events, JournalKind.QUIZ_PASSED),
                count(events, JournalKind.QUIZ_FAILED),
                minutes,
                activeDays,
                events.size());
    }

    private static long count(List<JournalEvent> events, JournalKind kind) {
        return events.stream().filter(event -> event.getKind() == kind).count();
    }

    /** Familles dans l'ordre de l'énumération : un rapport se compare d'un mois à l'autre. */
    private static List<ReportTally> families(List<JournalEvent> events) {
        Map<JournalFamily, Long> counts = new EnumMap<>(JournalFamily.class);
        events.forEach(event -> counts.merge(event.family(), 1L, Long::sum));
        List<ReportTally> tallies = new ArrayList<>();
        for (JournalFamily family : JournalFamily.values()) {
            long count = counts.getOrDefault(family, 0L);
            if (count > 0) {
                tallies.add(new ReportTally(family.name(), family.displayName(), count));
            }
        }
        return tallies;
    }

    private static List<ReportTally> kinds(List<JournalEvent> events) {
        Map<JournalKind, Long> counts = new EnumMap<>(JournalKind.class);
        events.forEach(event -> counts.merge(event.getKind(), 1L, Long::sum));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<JournalKind, Long>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().name()))
                .limit(KIND_LIMIT)
                .map(entry -> new ReportTally(entry.getKey().name(), entry.getKey().displayName(), entry.getValue()))
                .toList();
    }

    private static List<ActivityReport.MachineLine> machines(ReportWindow window, List<Own> allOwns,
                                                            Map<Long, Box> boxesById) {
        Map<Long, List<Own>> perBox = new HashMap<>();
        allOwns.stream()
                .filter(own -> window.contains(own.ownedAt()))
                .forEach(own -> perBox.computeIfAbsent(own.boxId(), key -> new ArrayList<>()).add(own));

        List<ActivityReport.MachineLine> lines = new ArrayList<>();
        perBox.forEach((boxId, ownsOfBox) -> {
            Box box = boxesById.get(boxId);
            if (box == null) {
                // Machine supprimée depuis : ses validations restent comptées
                // dans les totaux, mais il n'y a plus de ligne à nommer.
                return;
            }
            Set<FlagKind> kinds = new HashSet<>();
            ownsOfBox.forEach(own -> kinds.add(own.kind()));
            lines.add(new ActivityReport.MachineLine(box.getSlug(), box.getName(),
                    box.getDifficulty().displayName(), ownsOfBox.size(),
                    ownsOfBox.stream().mapToLong(Own::points).sum(), kinds.size() == 2,
                    ownsOfBox.stream().map(Own::ownedAt).max(Comparator.naturalOrder()).orElseThrow()));
        });
        lines.sort(Comparator.comparing(ActivityReport.MachineLine::lastAt).reversed());
        return lines;
    }

    private static List<ActivityReport.CourseLine> courseLines(ReportWindow window,
                                                              List<SectionCompletion> allCompletions,
                                                              List<Course> catalogue) {
        Map<Long, Course> byId = new HashMap<>();
        catalogue.forEach(course -> byId.put(course.getId(), course));
        Map<Long, CourseSection> sectionsById = sectionsById(catalogue);

        Map<Long, List<SectionCompletion>> perCourse = new HashMap<>();
        allCompletions.stream()
                .filter(completion -> window.contains(completion.completedAt()))
                .forEach(completion -> perCourse.computeIfAbsent(completion.courseId(), key -> new ArrayList<>())
                        .add(completion));

        List<ActivityReport.CourseLine> lines = new ArrayList<>();
        perCourse.forEach((courseId, done) -> {
            Course course = byId.get(courseId);
            if (course == null) {
                return;
            }
            long minutes = done.stream()
                    .map(completion -> sectionsById.get(completion.sectionId()))
                    .filter(java.util.Objects::nonNull)
                    .mapToLong(CourseSection::minutes)
                    .sum();
            lines.add(new ActivityReport.CourseLine(course.getSlug(), course.getTitle(),
                    course.getTrack().displayName(), done.size(), minutes,
                    done.stream().map(SectionCompletion::completedAt).max(Comparator.naturalOrder()).orElseThrow()));
        });
        lines.sort(Comparator.comparing(ActivityReport.CourseLine::lastAt).reversed());
        return lines;
    }

    private static Map<Long, CourseSection> sectionsById(List<Course> catalogue) {
        Map<Long, CourseSection> sections = new HashMap<>();
        catalogue.forEach(course -> course.getSections().forEach(section -> sections.put(section.id(), section)));
        return sections;
    }
}
