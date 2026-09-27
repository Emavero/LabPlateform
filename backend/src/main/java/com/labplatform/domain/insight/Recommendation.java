package com.labplatform.domain.insight;

import java.util.Objects;

/**
 * Une chose à faire, déduite de ce que les comptes ont réellement fait.
 *
 * @param code     identifiant stable de la règle : sert aux traductions et au
 *                 suivi, là où un libellé changerait à chaque reformulation
 * @param title    ce qui est constaté
 * @param advice   ce qu'il y a à faire
 * @param evidence le chiffre qui la motive, pour qu'elle ne se croie pas sur
 *                 parole — une recommandation sans preuve ne se vérifie pas
 * @param subject  la ressource concernée (machine, cours, filière), si une
 *                 seule est en cause
 */
public record Recommendation(String code, RecommendationSeverity severity, String title, String advice,
                             String evidence, String subject) {

    public Recommendation {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(advice, "advice");
        Objects.requireNonNull(evidence, "evidence");
    }

    public static Recommendation of(String code, RecommendationSeverity severity, String title, String advice,
                                    String evidence) {
        return new Recommendation(code, severity, title, advice, evidence, null);
    }

    public static Recommendation about(String code, RecommendationSeverity severity, String subject, String title,
                                       String advice, String evidence) {
        return new Recommendation(code, severity, title, advice, evidence, subject);
    }
}
