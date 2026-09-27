package com.labplatform.application.service;

import com.labplatform.application.port.in.billing.GetEffectivePlanUseCase;
import com.labplatform.application.port.in.exposure.GetLabExposureUseCase;
import com.labplatform.application.port.in.exposure.LabExposure;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.domain.billing.ProAccessPolicy;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.exposure.AttackPathFinder;
import com.labplatform.domain.exposure.ExposureAnalyzer;
import com.labplatform.domain.exposure.TargetExposure;
import com.labplatform.domain.exposure.TargetFacts;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.user.Actor;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Surface d'attaque du lab : ce qui est exposé, et par où l'on progresse.
 * <p>
 * Le service ne calcule rien lui-même. Il rassemble les faits — catalogue,
 * validations, journal — puis les passe au domaine, qui note et trace les
 * chemins. La conséquence pratique : la règle d'exposition se relit en un
 * fichier, sans traverser une requête ni un contrôleur.
 * <p>
 * Le point de vigilance est ailleurs : ce module lit tout le catalogue, donc il
 * pourrait révéler d'un coup ce que les fiches réservées ne montrent pas. C'est
 * la même porte que le catalogue qui décide ici — {@link ProAccessPolicy} — et
 * un test le vérifie, sans quoi le module deviendrait un contournement de
 * l'abonnement.
 */
public class ExposureService implements GetLabExposureUseCase {

    /** Fenêtre des mesures d'usage : au-delà, une plateforme a changé de contenu. */
    private static final int WINDOW_DAYS = 90;
    /** Chemins rendus : au-delà, une page de chemins ne se lit plus. */
    private static final int PATH_LIMIT = 6;
    private static final int SUBJECT_LIMIT = 200;

    private final BoxRepositoryPort boxes;
    private final OwnRepositoryPort owns;
    private final JournalPort journal;
    private final GetEffectivePlanUseCase plans;
    private final Clock clock;

    public ExposureService(BoxRepositoryPort boxes, OwnRepositoryPort owns, JournalPort journal,
                           GetEffectivePlanUseCase plans, Clock clock) {
        this.boxes = boxes;
        this.owns = owns;
        this.journal = journal;
        this.plans = plans;
        this.clock = clock;
    }

    @Override
    public LabExposure exposure(Actor actor) {
        boolean unlocked = ProAccessPolicy.granted(actor, plans.planOf(actor));
        List<TargetFacts> facts = facts();
        List<TargetExposure> targets = ExposureAnalyzer.analyse(facts, unlocked);

        Map<String, Long> perSegment = new HashMap<>();
        facts.stream().filter(fact -> !fact.segment().isBlank())
                .forEach(fact -> perSegment.merge(fact.segment(), 1L, Long::sum));
        List<LabExposure.SegmentTally> segments = perSegment.entrySet().stream()
                .map(entry -> new LabExposure.SegmentTally(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(LabExposure.SegmentTally::segment))
                .toList();

        int lockedOut = (int) targets.stream().filter(TargetExposure::locked).count();
        return new LabExposure(targets, AttackPathFinder.paths(targets, facts, PATH_LIMIT), segments, unlocked,
                lockedOut);
    }

    private List<TargetFacts> facts() {
        Instant now = clock.instant();
        Instant from = now.minus(WINDOW_DAYS, ChronoUnit.DAYS);
        Map<String, Long> refusals = tally(JournalKind.FLAG_REFUSED, from, now);
        Map<String, Long> views = tally(JournalKind.BOX_VIEWED, from, now);

        Map<Long, Long> userOwns = new HashMap<>();
        Map<Long, Long> rootOwns = new HashMap<>();
        for (OwnRepositoryPort.OwnTally tally : owns.tallyByBox()) {
            (tally.kind() == FlagKind.USER ? userOwns : rootOwns).put(tally.boxId(), tally.count());
        }

        return boxes.findAll().stream()
                .map(box -> toFacts(box, userOwns, rootOwns, refusals, views))
                .toList();
    }

    private static TargetFacts toFacts(Box box, Map<Long, Long> userOwns, Map<Long, Long> rootOwns,
                                       Map<String, Long> refusals, Map<String, Long> views) {
        return new TargetFacts(box.getSlug(), box.getName(), box.getOperatingSystem(), box.getDifficulty(),
                box.getIpAddress(), box.isRetired(), box.isProOnly(),
                userOwns.getOrDefault(box.getId(), 0L), rootOwns.getOrDefault(box.getId(), 0L),
                refusals.getOrDefault(box.getSlug(), 0L), views.getOrDefault(box.getSlug(), 0L));
    }

    private Map<String, Long> tally(JournalKind kind, Instant from, Instant to) {
        Map<String, Long> counts = new HashMap<>();
        journal.tallyBySubjectBetween(kind, from, to, SUBJECT_LIMIT)
                .forEach(tally -> counts.put(tally.key(), tally.count()));
        return counts;
    }
}
