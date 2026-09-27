package com.labplatform.adapter.in.web.dto;

import com.labplatform.adapter.in.web.Texts;
import com.labplatform.domain.report.ActivityReport;
import com.labplatform.domain.report.ReportTally;
import com.labplatform.domain.report.ReportTotals;

import java.time.Instant;
import java.util.List;

/** Formes exposées par le centre de rapports. */
public final class ReportDtos {

    private ReportDtos() {
    }

    /**
     * Totaux d'une période.
     *
     * @param quizSuccessPercent null quand aucun quiz n'a été rendu : l'affichage
     *                           rend alors un tiret, et non un zéro qui se lirait
     *                           « tout manqué »
     */
    public record TotalsResponse(long flags, long boxesPwned, long points, long sections, long quizzesPassed,
                                 long quizzesFailed, long minutesStudied, long activeDays, long events,
                                 Integer quizSuccessPercent) {

        static TotalsResponse from(ReportTotals totals) {
            int percent = totals.quizSuccessPercent();
            return new TotalsResponse(totals.flags(), totals.boxesPwned(), totals.points(), totals.sections(),
                    totals.quizzesPassed(), totals.quizzesFailed(), totals.minutesStudied(), totals.activeDays(),
                    totals.events(), percent < 0 ? null : percent);
        }
    }

    public record TallyResponse(String code, String label, long count) {

        static TallyResponse from(ReportTally tally) {
            return new TallyResponse(tally.code(), Texts.of(tally.label()), tally.count());
        }
    }

    public record MachineLineResponse(String slug, String name, String difficulty, long flags, long points,
                                      boolean pwned, Instant lastAt) {

        static MachineLineResponse from(ActivityReport.MachineLine line) {
            return new MachineLineResponse(line.slug(), line.name(), Texts.of(line.difficulty()), line.flags(),
                    line.points(), line.pwned(), line.lastAt());
        }
    }

    public record CourseLineResponse(String slug, String title, String track, long sections, long minutes,
                                     Instant lastAt) {

        static CourseLineResponse from(ActivityReport.CourseLine line) {
            return new CourseLineResponse(line.slug(), line.title(), Texts.of(line.track()), line.sections(),
                    line.minutes(), line.lastAt());
        }
    }

    public record ReportResponse(Instant from, Instant to, int days, TotalsResponse totals, TotalsResponse previous,
                                 List<TallyResponse> families, List<TallyResponse> kinds,
                                 List<MachineLineResponse> machines, List<CourseLineResponse> courses) {

        public static ReportResponse from(ActivityReport report) {
            return new ReportResponse(report.from(), report.to(), report.days(),
                    TotalsResponse.from(report.totals()), TotalsResponse.from(report.previous()),
                    report.families().stream().map(TallyResponse::from).toList(),
                    report.kinds().stream().map(TallyResponse::from).toList(),
                    report.machines().stream().map(MachineLineResponse::from).toList(),
                    report.courses().stream().map(CourseLineResponse::from).toList());
        }
    }
}
