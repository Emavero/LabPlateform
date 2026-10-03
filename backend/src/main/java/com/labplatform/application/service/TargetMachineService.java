package com.labplatform.application.service;

import com.labplatform.application.port.in.lab.ControlTargetMachineUseCase;
import com.labplatform.application.port.out.CloudInstancePort;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.user.Actor;

import java.time.Clock;

/**
 * Pilotage de la cible partagée.
 * <p>
 * Le service ne décide de rien sur l'état : c'est l'hébergeur qui le dit, et
 * l'adaptateur qui refuse un ordre impossible. Ce qui se joue ici, c'est ce
 * qu'on garde de la demande.
 * <p>
 * L'authentification n'est pas revérifiée : la chaîne de sécurité exige déjà une
 * session sur tout /api/** hors authentification, et un acteur ne se construit
 * pas sans elle. L'acteur sert donc à nommer l'auteur, pas à l'autoriser.
 * <p>
 * Démarrer et éteindre sont journalisés, consulter ne l'est pas : l'interface
 * interroge l'état toutes les quelques secondes pendant une transition, et
 * noter chacune de ces lectures noierait le journal sans rien apprendre.
 */
public class TargetMachineService implements ControlTargetMachineUseCase {

    private final CloudInstancePort instance;
    private final JournalPort journal;
    private final Clock clock;

    public TargetMachineService(CloudInstancePort instance, JournalPort journal, Clock clock) {
        this.instance = instance;
        this.journal = journal;
        this.clock = clock;
    }

    @Override
    public MachineState start(Actor actor) {
        MachineState state = instance.start();
        record(actor, JournalKind.BOX_SPAWNED);
        return state;
    }

    @Override
    public MachineState stop(Actor actor) {
        MachineState state = instance.stop();
        record(actor, JournalKind.BOX_STOPPED);
        return state;
    }

    /** L'acteur n'est pas lu ici : la lecture n'est pas journalisée, et la
     *  session a déjà été exigée en amont. Le paramètre reste pour que les trois
     *  gestes du port se ressemblent. */
    @Override
    public MachineState state(Actor actor) {
        return instance.state();
    }

    private void record(Actor actor, JournalKind kind) {
        journal.record(JournalEvent.of(actor.userId(), kind, "cible-partagee", clock.instant()));
    }
}
