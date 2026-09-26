package com.labplatform.application.service;

import com.labplatform.application.port.in.box.BoxView;
import com.labplatform.application.port.in.box.FlagSubmissionResult;
import com.labplatform.application.port.in.box.GetBoxUseCase;
import com.labplatform.application.port.in.box.ListBoxesUseCase;
import com.labplatform.application.port.in.box.RateBoxUseCase;
import com.labplatform.application.port.in.box.SubmitFlagUseCase;
import com.labplatform.application.port.in.billing.GetEffectivePlanUseCase;
import com.labplatform.application.port.in.scoring.GetPlayerProgressUseCase;
import com.labplatform.application.port.out.BoxInstanceRepositoryPort;
import com.labplatform.application.port.out.BoxRatingRepositoryPort;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.domain.billing.Plan;
import com.labplatform.domain.billing.ProAccessPolicy;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.BoxInstance;
import com.labplatform.domain.box.BoxRating;
import com.labplatform.domain.box.CommunityRating;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.shared.PaymentRequiredException;
import com.labplatform.domain.user.Actor;

import java.time.Clock;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catalogue de machines et validation des flags.
 * <p>
 * Le service ne compare jamais un flag lui-même : il vérifie ce qui relève
 * de l'application (la machine existe, le joueur n'a pas déjà validé ce
 * flag, personne ne l'avait validé avant lui) et laisse l'agrégat
 * {@link Box} juger la soumission et fixer les points.
 * <p>
 * C'est aussi ici que se joue la réserve aux abonnés. Le catalogue reste
 * visible de tous — on ne vend pas ce qu'on cache — mais une machine réservée
 * se présente verrouillée, et toute action dessus (soumettre un flag, lancer
 * la cible, noter) est refusée : la règle est appliquée sur l'action, pas
 * seulement sur l'affichage.
 */
public class BoxService implements ListBoxesUseCase, GetBoxUseCase, SubmitFlagUseCase, RateBoxUseCase {

    /** Les plus récentes d'abord, comme sur la page d'accueil d'une plateforme de challenges. */
    private static final Comparator<Box> DISPLAY_ORDER =
            Comparator.comparing(Box::getReleasedAt).reversed().thenComparing(Box::getName);

    private final BoxRepositoryPort boxes;
    private final OwnRepositoryPort owns;
    private final BoxRatingRepositoryPort ratings;
    private final BoxInstanceRepositoryPort instances;
    private final GetPlayerProgressUseCase progress;
    private final GetEffectivePlanUseCase plans;
    private final JournalPort journal;
    private final TransactionPort transactions;
    private final Clock clock;

    public BoxService(BoxRepositoryPort boxes, OwnRepositoryPort owns, BoxRatingRepositoryPort ratings,
                      BoxInstanceRepositoryPort instances, GetPlayerProgressUseCase progress,
                      GetEffectivePlanUseCase plans, JournalPort journal, TransactionPort transactions,
                      Clock clock) {
        this.boxes = boxes;
        this.owns = owns;
        this.ratings = ratings;
        this.instances = instances;
        this.progress = progress;
        this.plans = plans;
        this.journal = journal;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<BoxView> listBoxes(Actor actor) {
        List<Own> mine = owns.findByUser(actor.userId());
        Map<Long, CommunityRating> perceived = perceivedDifficulties();
        // Une seule lecture de la formule pour toute la liste.
        Plan plan = plans.planOf(actor);
        return boxes.findAll().stream()
                .sorted(DISPLAY_ORDER)
                .map(box -> view(actor, box, mine, perceived, plan))
                .toList();
    }

    /**
     * Fiche d'une machine. La consultation n'est pas inscrite ici : ce cas
     * d'usage sert aussi à rendre l'état après un lancement de cible ou une
     * note, et compter ces appels comme des visites faussait le classement des
     * machines les plus regardées. C'est l'adaptateur HTTP, qui sait qu'une
     * requête est bien une consultation, qui l'inscrit.
     */
    @Override
    public BoxView getBox(Actor actor, String slug) {
        Box box = require(slug);
        return view(actor, box, owns.findByUser(actor.userId()), perceivedDifficulties(), plans.planOf(actor));
    }

    @Override
    public BoxView rateBox(Actor actor, String slug, Difficulty difficulty) {
        Box box = require(slug);
        requireAccess(actor, box);
        List<Own> mine = owns.findByUser(actor.userId());
        boolean pwned = BoxView.of(box, mine).isPwned();

        transactions.inTransaction(() -> ratings.save(
                BoxRating.cast(actor.userId(), box.getId(), difficulty, pwned, clock.instant())));
        journal.record(JournalEvent.of(actor.userId(), JournalKind.BOX_RATED, box.getSlug(), difficulty.name(),
                clock.instant()));

        return view(actor, box, mine, perceivedDifficulties(), plans.planOf(actor));
    }

    @Override
    public FlagSubmissionResult submitFlag(Actor actor, String slug, FlagKind kind, String flag) {
        // Format vérifié avant toute lecture en base : une saisie absurde ne déclenche aucune requête.
        Flag.requireWellFormed(flag);
        Box box = require(slug);
        requireAccess(actor, box);

        Own saved;
        try {
            saved = transactions.inTransaction(() -> {
                if (owns.exists(actor.userId(), box.getId(), kind)) {
                    throw new ConflictException("Vous avez déjà validé ce flag");
                }
                boolean firstBlood = owns.noneYet(box.getId(), kind);
                return owns.save(box.claim(actor.userId(), kind, flag, firstBlood, clock.instant()));
            });
        } catch (InvalidInputException wrongFlag) {
            // Les refus sont inscrits eux aussi : c'est ce qui permet de voir
            // où les joueurs butent, et donc quelle machine mérite un indice.
            journal.record(JournalEvent.of(actor.userId(), JournalKind.FLAG_REFUSED, box.getSlug(), kind.name(),
                    clock.instant()));
            throw wrongFlag;
        }

        boolean pwned = kind == FlagKind.ROOT
                ? owns.exists(actor.userId(), box.getId(), FlagKind.USER)
                : owns.exists(actor.userId(), box.getId(), FlagKind.ROOT);

        journal.record(JournalEvent.of(actor.userId(), JournalKind.FLAG_VALIDATED, box.getSlug(), kind.name(),
                clock.instant()));
        if (pwned) {
            journal.record(JournalEvent.of(actor.userId(), JournalKind.BOX_PWNED, box.getSlug(), clock.instant()));
        }

        return new FlagSubmissionResult(box.getSlug(), box.getName(), kind, saved.points(), saved.firstBlood(),
                pwned, progress.progressOf(actor));
    }

    private BoxView view(Actor actor, Box box, List<Own> mine, Map<Long, CommunityRating> perceived, Plan plan) {
        boolean locked = isLocked(actor, box, plan);
        Difficulty myVote = ratings.find(actor.userId(), box.getId()).map(BoxRating::difficulty).orElse(null);
        return BoxView.of(box, mine, perceived.getOrDefault(box.getId(), CommunityRating.NONE), myVote,
                locked ? null : instanceOf(actor, box), locked);
    }

    private static boolean isLocked(Actor actor, Box box, Plan plan) {
        return box.isProOnly() && !ProAccessPolicy.granted(actor, plan);
    }

    /**
     * Refuse l'action sur une machine réservée. Levé avant toute écriture :
     * un non-abonné ne doit pas pouvoir deviner un flag par tâtonnement, ni
     * occuper un emplacement de cible.
     */
    private void requireAccess(Actor actor, Box box) {
        if (!box.isProOnly()) {
            return;
        }
        try {
            ProAccessPolicy.requirePro(actor, plans.planOf(actor));
        } catch (PaymentRequiredException locked) {
            // Une envie non servie : le tableau de bord en fait un indicateur.
            journal.record(JournalEvent.of(actor.userId(), JournalKind.BOX_LOCKED_OUT, box.getSlug(),
                    clock.instant()));
            throw locked;
        }
    }

    /**
     * Une cible échue est présentée comme arrêtée sans attendre : l'extinction
     * réelle a lieu au prochain lancement (voir BoxInstanceService).
     */
    private BoxInstance instanceOf(Actor actor, Box box) {
        return instances.find(actor.userId(), box.getId())
                .map(instance -> instance.isExpiredAt(clock.instant())
                        ? BoxInstance.idle(actor.userId(), box.getId())
                        : instance)
                .orElse(null);
    }

    /** Dépouillement en une requête, puis une moyenne par machine. */
    private Map<Long, CommunityRating> perceivedDifficulties() {
        Map<Long, Map<Difficulty, Integer>> byBox = new HashMap<>();
        for (BoxRatingRepositoryPort.Tally tally : ratings.tallies()) {
            byBox.computeIfAbsent(tally.boxId(), id -> new EnumMap<>(Difficulty.class))
                    .merge(tally.difficulty(), tally.votes(), Integer::sum);
        }
        Map<Long, CommunityRating> perceived = new HashMap<>();
        byBox.forEach((boxId, votes) -> perceived.put(boxId, CommunityRating.of(votes)));
        return perceived;
    }

    private Box require(String slug) {
        return boxes.findBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Machine introuvable"));
    }
}
