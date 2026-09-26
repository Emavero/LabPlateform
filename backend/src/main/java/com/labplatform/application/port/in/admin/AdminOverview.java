package com.labplatform.application.port.in.admin;

/**
 * Ce que l'administrateur voit d'un coup d'œil : la taille du contenu qu'il
 * publie, et l'usage qu'en font les comptes.
 */
public record AdminOverview(long users, long boxes, long courses, long sections, long flagsValidated,
                            long sectionsCompleted) {
}
