package com.labplatform.adapter.out.gcp;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.Locale;

/**
 * Nom d'instance conforme à ce que Compute Engine accepte.
 * <p>
 * La règle est celle de la RFC 1035 : de 1 à 63 caractères, minuscules,
 * chiffres et traits d'union, commençant par une lettre et ne finissant pas par
 * un trait d'union. Un nom refusé ne se voit qu'au moment de créer l'instance,
 * c'est-à-dire devant un apprenant qui vient de cliquer sur « Démarrer » : la
 * correction est faite ici, une fois, plutôt que laissée à la chance du
 * nommage des machines du catalogue.
 * <p>
 * Le nom est <strong>dérivé</strong>, jamais saisi : il vient du gabarit de
 * configuration, de l'identifiant d'URL de la machine et de celui de
 * l'apprenant. On corrige donc ce qui peut l'être silencieusement, et on refuse
 * seulement ce qui ne laisse rien à corriger.
 */
public final class InstanceName {

    private static final int MAX_LENGTH = 63;

    private InstanceName() {
    }

    /**
     * Nom de la cible d'un apprenant, rendu depuis le gabarit de configuration.
     * <p>
     * Quand le nom dépasse la longueur permise, c'est <strong>l'identifiant de
     * la machine</strong> qui est raccourci, jamais celui de l'apprenant. Une
     * troncature aveugle du nom entier donnerait le même nom à deux apprenants
     * dont le slug est long — ils partageraient alors une instance, ce que ce
     * modèle existe précisément pour éviter.
     */
    public static String forTarget(String template, String boxSlug, Long userId) {
        if (template == null || template.isBlank()) {
            throw new InvalidInputException("Nom d'instance vide : vérifiez le gabarit app.gcp.target-name");
        }
        String user = String.valueOf(userId);
        String slug = boxSlug == null ? "" : boxSlug;
        int overflow = render(template, slug, user).length() - MAX_LENGTH;
        if (overflow > 0) {
            slug = slug.length() > overflow ? slug.substring(0, slug.length() - overflow) : "";
        }
        return of(render(template, slug, user));
    }

    private static String render(String template, String slug, String user) {
        return template.replace("{slug}", slug).replace("{user}", user);
    }

    /**
     * Rend le nom acceptable par Compute Engine, ou échoue s'il ne reste rien à
     * en tirer.
     */
    public static String of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException("Nom d'instance vide : vérifiez le gabarit app.gcp.target-name");
        }
        StringBuilder cleaned = new StringBuilder(raw.length());
        for (char c : raw.toLowerCase(Locale.ROOT).toCharArray()) {
            cleaned.append(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' ? c : '-');
        }
        // Compute Engine veut une lettre en tête ; un gabarit qui commence par
        // un chiffre est réparable, inutile d'en faire une panne.
        if (cleaned.charAt(0) < 'a' || cleaned.charAt(0) > 'z') {
            cleaned.insert(0, 'x');
        }
        if (cleaned.length() > MAX_LENGTH) {
            cleaned.setLength(MAX_LENGTH);
        }
        while (cleaned.length() > 1 && cleaned.charAt(cleaned.length() - 1) == '-') {
            cleaned.setLength(cleaned.length() - 1);
        }
        return cleaned.toString();
    }
}
