package com.labplatform.application.port.in.box;

import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.scoring.PlayerProgress;

/**
 * Résultat d'une soumission acceptée. La progression à jour est renvoyée
 * avec : l'interface affiche le gain sans avoir à recharger le profil.
 *
 * @param firstBlood premier joueur à valider ce flag
 * @param pwned      la machine est désormais possédée de bout en bout
 */
public record FlagSubmissionResult(String boxSlug, String boxName, FlagKind kind, int pointsAwarded,
                                   boolean firstBlood, boolean pwned, PlayerProgress progress) {
}
