package com.labplatform.application.port.in.admin;

import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.lab.OperatingSystem;

/**
 * Machine du catalogue telle que l'administrateur la saisit.
 * <p>
 * {@code proOnly} réserve la machine aux abonnés : c'est le seul endroit d'où
 * cette décision vient, et elle est vraie par défaut côté agrégat.
 * <p>
 * Les flags sont facultatifs : laissés vides, ils sont tirés au hasard à la
 * création et laissés inchangés à la modification. Les renseigner sert à
 * refléter ceux qui ont réellement été déposés sur la cible.
 */
public record BoxDraft(String name, OperatingSystem operatingSystem, Difficulty difficulty, String synopsis,
                       String ipAddress, String maker, boolean retired, boolean proOnly, String userFlag,
                       String rootFlag) {
}
