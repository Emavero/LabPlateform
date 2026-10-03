package com.labplatform.adapter.in.web.dto;

import com.labplatform.adapter.in.web.Texts;
import com.labplatform.domain.lab.MachineState;

/** Représentation HTTP de l'état de la cible partagée. */
public final class MachineDtos {

    private MachineDtos() {
    }

    /**
     * État de la machine.
     * <p>
     * {@code transitioning} est calculé par le serveur plutôt que déduit du
     * code par le client : c'est le domaine qui sait quels états sont de
     * passage, et c'est sur ce champ que l'interface décide de désactiver le
     * bouton et de redemander.
     *
     * @param status        code de l'état, stable (PROVISIONING, STAGING, RUNNING, STOPPING, TERMINATED)
     * @param statusName    libellé traduit, pour l'affichage
     * @param transitioning l'état changera de lui-même
     * @param internalIp    adresse interne, présente seulement quand la machine tourne
     */
    public record MachineStateResponse(String status, String statusName, boolean transitioning, String internalIp) {

        public static MachineStateResponse from(MachineState state) {
            return new MachineStateResponse(
                    state.status().name(),
                    Texts.of(label(state)),
                    state.status().isTransitional(),
                    state.internalIp());
        }

        private static String label(MachineState state) {
            return switch (state.status()) {
                case PROVISIONING -> "Création en cours";
                case STAGING -> "Démarrage en cours";
                case RUNNING -> "En marche";
                case STOPPING -> "Extinction en cours";
                case TERMINATED -> "Éteinte";
            };
        }
    }
}
