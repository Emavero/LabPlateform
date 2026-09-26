package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.JournalDtos;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.journal.GetJournalUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Journal de toute la plateforme. Sous {@code /api/admin}, donc filtré par la
 * chaîne de sécurité, et revérifié par le cas d'usage.
 */
@RestController
@RequestMapping("/api/admin/journal")
public class AdminJournalController {

    private final GetJournalUseCase journal;

    public AdminJournalController(GetJournalUseCase journal) {
        this.journal = journal;
    }

    @GetMapping
    public List<JournalDtos.JournalLineResponse> platform(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @RequestParam(defaultValue = "100") int limit) {
        return journal.platformJournal(user.toActor(), limit).stream()
                .map(JournalDtos.JournalLineResponse::from)
                .toList();
    }
}
