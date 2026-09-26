package com.labplatform.application.service;

import com.labplatform.application.port.in.billing.GetEffectivePlanUseCase;
import com.labplatform.application.port.in.box.BoxView;
import com.labplatform.application.port.in.box.GetBoxUseCase;
import com.labplatform.application.port.in.box.SpawnBoxUseCase;
import com.labplatform.application.port.out.BoxInstanceRepositoryPort;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.HypervisorPort;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.domain.billing.ProAccessPolicy;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.BoxInstance;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Cibles lancées à la demande.
 * <p>
 * Deux règles. Un joueur ne fait tourner qu'une cible à la fois : la
 * plateforme n'est pas un hébergeur, et rien n'oblige à garder trois machines
 * allumées pour en attaquer une. Et une cible a une durée de vie : à la
 * première lecture qui suit son échéance, elle est éteinte, sans quoi une
 * machine oubliée resterait indéfiniment.
 * <p>
 * Comme pour les machines d'attaque, l'appel à l'infrastructure est fait hors
 * transaction : lancer un conteneur prend plusieurs secondes.
 */
public class BoxInstanceService implements SpawnBoxUseCase {

    private final BoxRepositoryPort boxes;
    private final BoxInstanceRepositoryPort instances;
    private final HypervisorPort hypervisor;
    private final GetBoxUseCase boxView;
    private final GetEffectivePlanUseCase plans;
    private final JournalPort journal;
    private final TransactionPort transactions;
    private final Clock clock;
    private final Duration lifetime;

    public BoxInstanceService(BoxRepositoryPort boxes, BoxInstanceRepositoryPort instances, HypervisorPort hypervisor,
                              GetBoxUseCase boxView, GetEffectivePlanUseCase plans, JournalPort journal,
                              TransactionPort transactions, Clock clock, Duration lifetime) {
        this.boxes = boxes;
        this.instances = instances;
        this.hypervisor = hypervisor;
        this.boxView = boxView;
        this.plans = plans;
        this.journal = journal;
        this.transactions = transactions;
        this.clock = clock;
        this.lifetime = lifetime;
    }

    @Override
    public BoxView spawn(Actor actor, String slug) {
        Box box = require(slug);
        // Vérifié avant d'allumer quoi que ce soit : une cible réservée ne
        // consomme pas de ressources pour un compte qui n'y a pas droit.
        if (box.isProOnly()) {
            ProAccessPolicy.requirePro(actor, plans.planOf(actor));
        }
        expireOutdated(actor.userId());

        Optional<BoxInstance> running = instances.findRunningByUser(actor.userId());
        if (running.isPresent()) {
            if (running.get().getBoxId().equals(box.getId())) {
                throw new ConflictException("Cette machine tourne déjà");
            }
            throw new ConflictException("Une autre machine tourne déjà. Arrêtez-la avant d'en lancer une autre.");
        }

        String address = hypervisor.powerOnTarget(box, actor.userId());

        transactions.inTransaction(() -> {
            BoxInstance instance = instances.find(actor.userId(), box.getId())
                    .orElseGet(() -> BoxInstance.idle(actor.userId(), box.getId()));
            instance.markStarted(address, clock.instant(), lifetime);
            return instances.save(instance);
        });
        journal.record(JournalEvent.of(actor.userId(), JournalKind.BOX_SPAWNED, box.getSlug(), clock.instant()));
        return boxView.getBox(actor, slug);
    }

    @Override
    public BoxView stop(Actor actor, String slug) {
        Box box = require(slug);
        Optional<BoxInstance> instance = instances.find(actor.userId(), box.getId())
                .filter(BoxInstance::isRunning);

        if (instance.isPresent()) {
            hypervisor.powerOffTarget(box, actor.userId());
            transactions.inTransaction(() -> {
                BoxInstance current = instance.get();
                current.markStopped();
                return instances.save(current);
            });
            journal.record(JournalEvent.of(actor.userId(), JournalKind.BOX_STOPPED, box.getSlug(), clock.instant()));
        }
        return boxView.getBox(actor, slug);
    }

    /** Éteint les cibles dont l'échéance est passée. Appelé avant toute décision. */
    private void expireOutdated(Long userId) {
        List<BoxInstance> outdated = instances.findByUser(userId).stream()
                .filter(instance -> instance.isExpiredAt(clock.instant()))
                .toList();
        for (BoxInstance instance : outdated) {
            boxes.findAll().stream()
                    .filter(box -> box.getId().equals(instance.getBoxId()))
                    .findFirst()
                    .ifPresent(box -> hypervisor.powerOffTarget(box, userId));
            transactions.inTransaction(() -> {
                instance.markStopped();
                return instances.save(instance);
            });
        }
    }

    private Box require(String slug) {
        return boxes.findBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Machine introuvable"));
    }
}
