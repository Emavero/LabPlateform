package com.labplatform.domain.writeup;

import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;

import java.time.Instant;
import java.util.Objects;

/**
 * Compte rendu de compromission écrit par un joueur.
 * <p>
 * Il ne s'écrit qu'une fois la machine possédée de bout en bout : avant, on
 * n'a pas de quoi rendre compte. Publier le rend lisible par les autres
 * joueurs qui l'ont possédée eux aussi — jamais par ceux qui la cherchent
 * encore, sans quoi la plateforme distribuerait les solutions.
 */
public class Writeup {

    private static final int MAX_TITLE_LENGTH = 128;
    private static final int MAX_CONTENT_LENGTH = 40_000;

    private final Long id;
    private final Long boxId;
    private final Long authorId;
    private String title;
    private String content;
    private boolean published;
    private final Instant createdAt;
    private Instant updatedAt;

    private Writeup(Long id, Long boxId, Long authorId, String title, String content, boolean published,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.boxId = Objects.requireNonNull(boxId, "boxId");
        this.authorId = Objects.requireNonNull(authorId, "authorId");
        this.title = requireTitle(title);
        this.content = requireContent(content);
        this.published = published;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** Premier jet. Refusé tant que la machine n'est pas possédée. */
    public static Writeup write(Long authorId, Long boxId, String title, String content, boolean published,
                                boolean pwned, Instant now) {
        requirePwned(pwned);
        return new Writeup(null, boxId, authorId, title, content, published, now, now);
    }

    public static Writeup restore(Long id, Long boxId, Long authorId, String title, String content,
                                  boolean published, Instant createdAt, Instant updatedAt) {
        return new Writeup(Objects.requireNonNull(id, "id"), boxId, authorId, title, content, published, createdAt,
                updatedAt);
    }

    /** Réécriture par son auteur. La possession reste exigée : elle peut avoir été révoquée. */
    public void revise(String title, String content, boolean published, boolean pwned, Instant now) {
        requirePwned(pwned);
        this.title = requireTitle(title);
        this.content = requireContent(content);
        this.published = published;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public boolean isWrittenBy(Long userId) {
        return authorId.equals(userId);
    }

    /**
     * Lisible par ce joueur ? Le sien toujours ; celui d'un autre seulement
     * s'il est publié et que le lecteur a lui aussi possédé la machine.
     */
    public boolean isReadableBy(Long userId, boolean readerHasPwned) {
        return isWrittenBy(userId) || (published && readerHasPwned);
    }

    private static void requirePwned(boolean pwned) {
        if (!pwned) {
            throw new ConflictException("Rédigez votre compte rendu une fois les deux flags validés");
        }
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidInputException("Le titre est obligatoire");
        }
        String trimmed = title.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new InvalidInputException("Le titre est limité à " + MAX_TITLE_LENGTH + " caractères");
        }
        return trimmed;
    }

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidInputException("Le compte rendu est vide");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidInputException("Le compte rendu est limité à " + MAX_CONTENT_LENGTH + " caractères");
        }
        return content;
    }

    public Long getId() {
        return id;
    }

    public Long getBoxId() {
        return boxId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isPublished() {
        return published;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
