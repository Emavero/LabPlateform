package com.labplatform.adapter.out.notification;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Le message envoyé pour une réinitialisation de mot de passe.
 * <p>
 * Séparé de l'envoi, et sans dépendance à Spring : toute la matière du courriel
 * — le lien, le délai, la formulation — se teste sans serveur SMTP ni faux
 * objet. L'adaptateur qui l'expédie ne fait plus que de la plomberie.
 * <p>
 * Texte brut et non HTML : un lien de réinitialisation n'a rien à mettre en
 * forme, le texte brut traverse tous les clients de messagerie sans surprise,
 * et il évite qu'un filtre anti-hameçonnage réécrive le lien.
 */
public record ResetMailContent(String subject, String body) {

    private static final String PATH = "/reset-password?token=";

    /**
     * Compose le message dans la langue demandée.
     *
     * @param publicUrl adresse publique de l'application, telle que la voit
     *                  l'utilisateur : c'est elle qui fait le lien cliquable,
     *                  pas l'adresse interne du conteneur
     * @param now       pour exprimer le délai restant en minutes plutôt qu'en
     *                  heure absolue, qu'il faudrait traduire en fuseau
     */
    public static ResetMailContent of(String publicUrl, String rawToken, Instant expiresAt, Instant now,
                                      Locale locale) {
        String link = link(publicUrl, rawToken);
        // Arrondi et non troncature : quelques centièmes de seconde séparent la
        // création du jeton de l'envoi, et un lien valable un quart d'heure
        // s'annoncerait « 14 minutes ». Jamais moins d'une minute.
        long minutes = Math.max(1, Math.round(Duration.between(now, expiresAt).toSeconds() / 60.0));
        return english(locale)
                ? new ResetMailContent("cyberMans — reset your password", englishBody(link, minutes))
                : new ResetMailContent("cyberMans — réinitialisation de votre mot de passe", frenchBody(link, minutes));
    }

    /** Le français est la langue de référence : toute autre demande y retombe. */
    private static boolean english(Locale locale) {
        return locale != null && "en".equalsIgnoreCase(locale.getLanguage());
    }

    static String link(String publicUrl, String rawToken) {
        String base = publicUrl == null ? "" : publicUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + PATH + URLEncoder.encode(rawToken == null ? "" : rawToken, StandardCharsets.UTF_8);
    }

    private static String frenchBody(String link, long minutes) {
        return """
                Bonjour,

                Vous avez demandé à réinitialiser votre mot de passe sur cyberMans.
                Ouvrez ce lien pour en choisir un nouveau :

                %s

                Ce lien est valable %d minutes. Passé ce délai, il faudra en demander un autre.

                Si vous n'êtes pas à l'origine de cette demande, ignorez ce message :
                votre mot de passe reste inchangé.

                — cyberMans
                """.formatted(link, minutes);
    }

    private static String englishBody(String link, long minutes) {
        return """
                Hello,

                You asked to reset your cyberMans password.
                Open this link to choose a new one:

                %s

                The link is valid for %d minutes. After that, you will need to ask for another one.

                If you did not ask for this, ignore this message: your password stays unchanged.

                — cyberMans
                """.formatted(link, minutes);
    }
}
