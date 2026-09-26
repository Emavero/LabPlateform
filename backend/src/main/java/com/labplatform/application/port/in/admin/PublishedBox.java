package com.labplatform.application.port.in.admin;

import com.labplatform.domain.box.Box;

/**
 * Machine publiée, accompagnée des flags en clair **une seule fois** : au
 * moment où ils viennent d'être tirés, pour que l'administrateur puisse les
 * déposer sur la cible. Ils ne sont conservés nulle part et ne seront plus
 * jamais lisibles ensuite.
 *
 * @param userFlagOnce flag utilisateur en clair, nul s'il n'a pas changé
 * @param rootFlagOnce flag root en clair, nul s'il n'a pas changé
 */
public record PublishedBox(Box box, String userFlagOnce, String rootFlagOnce) {
}
