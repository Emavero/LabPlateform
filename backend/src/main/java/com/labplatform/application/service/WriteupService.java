package com.labplatform.application.service;

import com.labplatform.application.port.in.writeup.ListWriteupsUseCase;
import com.labplatform.application.port.in.writeup.WriteWriteupUseCase;
import com.labplatform.application.port.in.writeup.WriteupView;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.scoring.Handle;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.User;
import com.labplatform.domain.writeup.Writeup;
import com.labplatform.application.port.out.WriteupRepositoryPort;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Comptes rendus de compromission.
 * <p>
 * La règle de lecture est le point sensible : publier ne rend pas un compte
 * rendu public, cela le rend lisible par ceux qui ont eux aussi possédé la
 * machine. Sinon la plateforme distribuerait les solutions à ceux qui
 * cherchent encore.
 */
public class WriteupService implements ListWriteupsUseCase, WriteWriteupUseCase {

    /** Le sien d'abord, puis les plus récemment mis à jour. */
    private static final Comparator<WriteupView> DISPLAY_ORDER =
            Comparator.comparing(WriteupView::mine).reversed()
                    .thenComparing(view -> view.writeup().getUpdatedAt(), Comparator.reverseOrder());

    private final BoxRepositoryPort boxes;
    private final OwnRepositoryPort owns;
    private final WriteupRepositoryPort writeups;
    private final UserRepositoryPort users;
    private final JournalPort journal;
    private final TransactionPort transactions;
    private final Clock clock;

    public WriteupService(BoxRepositoryPort boxes, OwnRepositoryPort owns, WriteupRepositoryPort writeups,
                          UserRepositoryPort users, JournalPort journal, TransactionPort transactions, Clock clock) {
        this.boxes = boxes;
        this.owns = owns;
        this.writeups = writeups;
        this.users = users;
        this.journal = journal;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<WriteupView> listWriteups(Actor actor, String slug) {
        Box box = require(slug);
        boolean pwned = hasPwned(actor.userId(), box.getId());
        return writeups.findByBox(box.getId()).stream()
                .filter(writeup -> writeup.isReadableBy(actor.userId(), pwned))
                .map(writeup -> view(writeup, actor))
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    @Override
    public WriteupView save(Actor actor, String slug, String title, String content, boolean published) {
        Box box = require(slug);
        boolean pwned = hasPwned(actor.userId(), box.getId());

        Writeup saved = transactions.inTransaction(() -> writeups.save(
                writeups.find(actor.userId(), box.getId())
                        .map(existing -> {
                            existing.revise(title, content, published, pwned, clock.instant());
                            return existing;
                        })
                        .orElseGet(() -> Writeup.write(actor.userId(), box.getId(), title, content, published,
                                pwned, clock.instant()))));
        if (saved.isPublished()) {
            journal.record(JournalEvent.of(actor.userId(), JournalKind.WRITEUP_PUBLISHED, box.getSlug(),
                    clock.instant()));
        }
        return view(saved, actor);
    }

    @Override
    public void delete(Actor actor, String slug) {
        Box box = require(slug);
        transactions.inTransaction(() -> writeups.delete(actor.userId(), box.getId()));
    }

    private WriteupView view(Writeup writeup, Actor actor) {
        String handle = users.findById(writeup.getAuthorId())
                .map(User::getEmail)
                .map(email -> Handle.fromEmail(email.value()))
                .orElse("anonyme");
        return new WriteupView(writeup, handle, writeup.isWrittenBy(actor.userId()));
    }

    private boolean hasPwned(Long userId, Long boxId) {
        return owns.exists(userId, boxId, FlagKind.USER) && owns.exists(userId, boxId, FlagKind.ROOT);
    }

    private Box require(String slug) {
        return boxes.findBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Machine introuvable"));
    }
}
