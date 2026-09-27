package com.labplatform.application.port.in.exposure;

import com.labplatform.domain.exposure.AttackPath;
import com.labplatform.domain.exposure.TargetExposure;

import java.util.List;

/**
 * Surface d'attaque du lab telle qu'elle est présentée à un lecteur.
 *
 * @param targets    cibles, les plus exposées d'abord
 * @param paths      chemins de progression, les objectifs les plus exigeants d'abord
 * @param segments   nombre de cibles par segment réseau
 * @param unlocked   le lecteur a-t-il accès au détail des cibles réservées ?
 * @param lockedOut  nombre de cibles dont le détail lui est masqué
 */
public record LabExposure(List<TargetExposure> targets, List<AttackPath> paths, List<SegmentTally> segments,
                          boolean unlocked, int lockedOut) {

    public record SegmentTally(String segment, long targets) {
    }
}
