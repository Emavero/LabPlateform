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

/** Journal d'activité du compte connecté. */
@RestController
@RequestMapping("/api/journal")
public class JournalController {

    private final GetJournalUseCase journal;

    public JournalController(GetJournalUseCase journal) {
        this.journal = journal;
    }

    @GetMapping
    public List<JournalDtos.JournalLineResponse> mine(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @RequestParam(defaultValue = "50") int limit) {
        return journal.myJournal(user.toActor(), limit).stream()
                .map(JournalDtos.JournalLineResponse::from)
                .toList();
    }
}
