package com.labplatform.application.service;

import com.labplatform.application.port.in.box.BoxView;
import com.labplatform.application.port.in.box.FlagSubmissionResult;
import com.labplatform.application.port.in.box.GetBoxUseCase;
import com.labplatform.application.port.in.box.ListBoxesUseCase;
import com.labplatform.application.port.in.box.SubmitFlagUseCase;
import com.labplatform.application.port.in.scoring.GetPlayerProgressUseCase;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;

import java.time.Clock;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;

/**
 * Catalogue de machines et validation des flags.
 * <p>
 * Le service ne compare jamais un flag lui-même : il vérifie ce qui relève
 * de l'application (la machine existe, le joueur n'a pas déjà validé ce
 * flag, personne ne l'avait validé avant lui) et laisse l'agrégat
 * {@link Box} juger la soumission et fixer les points.
 */
public class BoxService implements ListBoxesUseCase, GetBoxUseCase, SubmitFlagUseCase {

    /** Les plus récentes d'abord, comme sur la page d'accueil d'une plateforme de challenges. */
    private static final Comparator<Box> DISPLAY_ORDER =
            Comparator.comparing(Box::getReleasedAt).reversed().thenComparing(Box::getName);

    private final BoxRepositoryPort boxes;
    private final OwnRepositoryPort owns;
    private final GetPlayerProgressUseCase progress;
    private final TransactionPort transactions;
    private final Clock clock;

    public BoxService(BoxRepositoryPort boxes, OwnRepositoryPort owns, GetPlayerProgressUseCase progress,
                      TransactionPort transactions, Clock clock) {
        this.boxes = boxes;
        this.owns = owns;
        this.progress = progress;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<BoxView> listBoxes(Actor actor) {
        List<Own> mine = owns.findByUser(actor.userId());
        return boxes.findAll().stream()
                .sorted(DISPLAY_ORDER)
                .map(box -> BoxView.of(box, mine))
                .toList();
    }

    @Override
    public BoxView getBox(Actor actor, String slug) {
        return BoxView.of(require(slug), owns.findByUser(actor.userId()));
    }

    @Override
    public FlagSubmissionResult submitFlag(Actor actor, String slug, FlagKind kind, String flag) {
        // Format vérifié avant toute lecture en base : une saisie absurde ne déclenche aucune requête.
        Flag.requireWellFormed(flag);
        Box box = require(slug);

        Own saved = transactions.inTransaction(() -> {
            if (owns.exists(actor.userId(), box.getId(), kind)) {
                throw new ConflictException("Vous avez déjà validé ce flag");
            }
            boolean firstBlood = owns.noneYet(box.getId(), kind);
            return owns.save(box.claim(actor.userId(), kind, flag, firstBlood, clock.instant()));
        });

        boolean pwned = kind == FlagKind.ROOT
                ? owns.exists(actor.userId(), box.getId(), FlagKind.USER)
                : owns.exists(actor.userId(), box.getId(), FlagKind.ROOT);

        return new FlagSubmissionResult(box.getSlug(), box.getName(), kind, saved.points(), saved.firstBlood(),
                pwned, progress.progressOf(actor));
    }

    private Box require(String slug) {
        return boxes.findBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Machine introuvable"));
    }
}
