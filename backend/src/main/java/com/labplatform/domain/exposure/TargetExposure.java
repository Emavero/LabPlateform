package com.labplatform.domain.exposure;

import java.util.List;

/**
 * Exposition d'une cible : son niveau, ce qui l'explique, et ce qu'il y a à en
 * faire.
 *
 * @param address  adresse dans le lab, ou null quand le lecteur n'y a pas droit :
 *                 ce module ne doit pas devenir un contournement de l'abonnement
 * @param signals  raisons du niveau, la plus parlante d'abord
 * @param advice   conduite à tenir, formulée pour le lecteur
 */
public record TargetExposure(String slug, String name, ExposedService service, String segment, String address,
                             int score, ExposureLevel level, List<ExposureSignal> signals, String advice,
                             boolean locked) {
}
