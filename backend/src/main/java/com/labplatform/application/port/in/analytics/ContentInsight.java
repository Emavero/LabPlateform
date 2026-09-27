package com.labplatform.application.port.in.analytics;

import java.util.List;

/**
 * Un classement de contenus : ce qui attire, ce qui bloque.
 *
 * @param code    identifiant stable du classement, pour l'affichage et les traductions
 * @param title   ce que le classement montre
 * @param unit    ce que compte la colonne de droite (« consultations », « refus »…)
 * @param entries du plus important au moins important
 */
public record ContentInsight(String code, String title, String unit, List<Entry> entries) {

    public record Entry(String subject, long count) {
    }
}
