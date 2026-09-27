package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.ReportDtos.ReportResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.report.GetActivityReportUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rapport d'activité du compte connecté.
 * <p>
 * La période demandée est ramenée dans ses bornes par le domaine plutôt que
 * refusée : un rapport est une lecture, et un paramètre d'URL tordu ne doit pas
 * faire échouer une page.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final int DEFAULT_DAYS = 30;

    private final GetActivityReportUseCase reports;

    public ReportController(GetActivityReportUseCase reports) {
        this.reports = reports;
    }

    @GetMapping("/activity")
    public ReportResponse activity(@AuthenticationPrincipal AuthenticatedUser user,
                                  @RequestParam(defaultValue = "" + DEFAULT_DAYS) int days) {
        return ReportResponse.from(reports.report(user.toActor(), days));
    }
}
