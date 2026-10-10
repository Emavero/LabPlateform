package com.labplatform.domain.vpn;

import com.labplatform.domain.shared.InvalidInputException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Profil OpenVPN déposé par un administrateur, que tous les apprenants
 * téléchargent.
 * <p>
 * C'est l'autre façon de distribuer l'accès au lab : au lieu que la plateforme
 * émette un certificat par personne, l'administrateur fournit un fichier
 * produit par son propre serveur. Plus simple à mettre en place — aucune
 * autorité de certification à tenir — mais le même fichier pour tout le monde :
 * on ne peut plus révoquer l'accès d'une seule personne, seulement remplacer le
 * fichier pour tous. Le choix revient à l'exploitant, il est documenté.
 * <p>
 * Le contenu est validé à l'entrée plutôt qu'à la lecture : un fichier qui
 * n'est pas un profil client se signale au moment du dépôt, quand
 * l'administrateur peut encore corriger, et non des semaines plus tard quand un
 * apprenant n'arrive pas à se connecter.
 */
public record LabVpnProfile(String fileName, String content, Instant uploadedAt) {

    /** Un profil avec certificats intégrés pèse quelques kilo-octets ; au-delà, ce n'est pas un .ovpn. */
    public static final int MAX_SIZE_BYTES = 512 * 1024;

    private static final Pattern FILE_NAME = Pattern.compile("^[\\w .-]{1,96}\\.ovpn$");

    public LabVpnProfile {
        Objects.requireNonNull(uploadedAt, "uploadedAt");
        fileName = requireFileName(fileName);
        content = requireProfile(content);
    }

    public static LabVpnProfile of(String fileName, String content, Instant uploadedAt) {
        return new LabVpnProfile(fileName, content, uploadedAt);
    }

    public int sizeBytes() {
        return content.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Le profil route-t-il ce réseau ?
     * <p>
     * Sans route vers le réseau des machines, le tunnel monte et l'adresse
     * affichée par le bouton « Démarrer » ne répond pas — une panne silencieuse
     * qui ressemble à une machine en panne. Ce n'est pas une erreur pour autant :
     * le serveur OpenVPN peut pousser la route lui-même. D'où un avertissement,
     * et non un refus.
     */
    public boolean routes(LabNetwork network) {
        return content.lines()
                .map(String::strip)
                .anyMatch(line -> line.startsWith("route " + network.address())
                        || line.startsWith("redirect-gateway"));
    }

    private static String requireFileName(String name) {
        String trimmed = name == null ? "" : name.strip();
        if (!FILE_NAME.matcher(trimmed).matches()) {
            throw new InvalidInputException("Le fichier doit porter l'extension .ovpn");
        }
        return trimmed;
    }

    /**
     * Le contenu ressemble-t-il à un profil client ?
     * <p>
     * Trois vérifications, pas davantage : la plateforme n'a pas à juger de la
     * validité cryptographique d'un profil, c'est OpenVPN qui tranchera. Elle
     * écarte seulement ce qui ne peut manifestement pas en être un — un binaire,
     * un fichier vide, ou la configuration d'un serveur déposée à la place de
     * celle d'un client.
     */
    private static String requireProfile(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidInputException("Le fichier est vide");
        }
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_SIZE_BYTES) {
            throw new InvalidInputException("Le profil dépasse " + (MAX_SIZE_BYTES / 1024) + " Ko");
        }
        if (text.indexOf('\0') >= 0) {
            throw new InvalidInputException("Le fichier n'est pas un texte : ce n'est pas un profil OpenVPN");
        }
        List<String> lines = text.lines().map(String::strip).toList();
        boolean isClient = lines.stream().anyMatch(l -> l.equals("client") || l.startsWith("tls-client"));
        boolean hasRemote = lines.stream().anyMatch(l -> l.startsWith("remote "));
        if (!isClient || !hasRemote) {
            throw new InvalidInputException(
                    "Ce fichier n'est pas un profil client OpenVPN : il lui manque « client » ou « remote »");
        }
        return text;
    }
}
