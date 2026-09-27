package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.SupportDtos.TicketResponse;
import com.labplatform.adapter.in.web.dto.SupportDtos.TicketSummaryResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.support.ListTicketsUseCase;
import com.labplatform.application.port.in.support.OpenTicketUseCase;
import com.labplatform.application.port.in.support.ReplyToTicketUseCase;
import com.labplatform.domain.support.TicketCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Assistance vue du demandeur. Un administrateur passe par les mêmes routes
 * pour répondre : c'est le service qui décide si un message vient de l'équipe.
 */
@RestController
@RequestMapping("/api/support/tickets")
public class SupportController {

    private final OpenTicketUseCase openTicket;
    private final ListTicketsUseCase listTickets;
    private final ReplyToTicketUseCase replyToTicket;

    public SupportController(OpenTicketUseCase openTicket, ListTicketsUseCase listTickets,
                             ReplyToTicketUseCase replyToTicket) {
        this.openTicket = openTicket;
        this.listTickets = listTickets;
        this.replyToTicket = replyToTicket;
    }

    @GetMapping
    public List<TicketSummaryResponse> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return listTickets.listMine(user.toActor()).stream().map(TicketSummaryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse one(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return TicketResponse.from(listTickets.get(user.toActor(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse open(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody OpenRequest request) {
        return TicketResponse.from(openTicket.open(user.toActor(), request.category(), request.subject(),
                request.body()));
    }

    @PostMapping("/{id}/messages")
    public TicketResponse reply(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                @Valid @RequestBody ReplyRequest request) {
        return TicketResponse.from(replyToTicket.reply(user.toActor(), id, request.body()));
    }

    @PostMapping("/{id}/resolution")
    public TicketResponse resolve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return TicketResponse.from(replyToTicket.resolve(user.toActor(), id));
    }

    public record OpenRequest(
            @NotNull(message = "Le sujet de la demande est obligatoire") TicketCategory category,
            @NotBlank(message = "Le sujet est obligatoire")
            @Size(max = 140, message = "Le sujet est limité à 140 caractères") String subject,
            @NotBlank(message = "Le message est vide")
            @Size(max = 8_000, message = "Le message est limité à 8 000 caractères") String body) {
    }

    public record ReplyRequest(
            @NotBlank(message = "Le message est vide")
            @Size(max = 8_000, message = "Le message est limité à 8 000 caractères") String body) {
    }
}
