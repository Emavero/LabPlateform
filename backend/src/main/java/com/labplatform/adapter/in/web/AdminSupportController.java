package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.SupportDtos.QueueResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.support.GetSupportQueueUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** File d'attente de l'assistance : ce qui reste à traiter, et ce que ça dit. */
@RestController
@RequestMapping("/api/admin/support")
public class AdminSupportController {

    private final GetSupportQueueUseCase queue;

    public AdminSupportController(GetSupportQueueUseCase queue) {
        this.queue = queue;
    }

    @GetMapping("/queue")
    public QueueResponse queue(@AuthenticationPrincipal AuthenticatedUser user) {
        return QueueResponse.from(queue.queue(user.toActor()));
    }
}
