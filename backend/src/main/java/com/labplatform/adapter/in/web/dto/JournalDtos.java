package com.labplatform.adapter.in.web.dto;

import com.labplatform.application.port.in.journal.JournalLine;

import java.time.Instant;

/** Représentation HTTP du journal d'activité. */
public final class JournalDtos {

    private JournalDtos() {
    }

    /**
     * Une ligne du journal. Les libellés sont envoyés avec le code : le client
     * n'a pas à tenir une table de correspondance qui se désynchroniserait du
     * serveur à chaque nature ajoutée.
     */
    public record JournalLineResponse(String kind, String kindName, String family, String familyName, String handle,
                                     String subject, String detail, Instant at) {

        public static JournalLineResponse from(JournalLine line) {
            return new JournalLineResponse(
                    line.event().getKind().name(),
                    line.event().getKind().displayName(),
                    line.event().family().name(),
                    line.event().family().displayName(),
                    line.handle(),
                    line.event().getSubject().orElse(null),
                    line.event().getDetail().orElse(null),
                    line.event().getOccurredAt());
        }
    }
}
