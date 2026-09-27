package com.labplatform.domain.report;

/**
 * Décompte nommé d'un rapport : une famille d'usage, une nature d'acte, une
 * filière. Le libellé accompagne le code parce qu'un rapport se lit hors de
 * l'application — exporté, collé dans un message — où le code ne dirait rien.
 */
public record ReportTally(String code, String label, long count) {
}
