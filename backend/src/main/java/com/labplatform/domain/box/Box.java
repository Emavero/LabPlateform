package com.labplatform.domain.box;

import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.shared.InvalidInputException;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Machine du catalogue : une cible partagée, joignable dans le réseau du lab
 * une fois le VPN monté, que chacun tente de compromettre pour en soumettre
 * les deux flags.
 * <p>
 * À ne pas confondre avec {@link com.labplatform.domain.lab.VirtualMachine},
 * qui est la machine d'attaque personnelle de l'utilisateur.
 * <p>
 * L'agrégat est le seul à pouvoir transformer une soumission en possession :
 * c'est lui qui détient les flags et qui applique le barème.
 */
public class Box {

    private static final Pattern SLUG_FORMAT = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final int MAX_NAME_LENGTH = 64;

    private final Long id;
    private final String slug;
    private final String name;
    private final OperatingSystem operatingSystem;
    private final Difficulty difficulty;
    private final String synopsis;
    private final String ipAddress;
    private final String maker;
    private final Instant releasedAt;
    private final boolean retired;
    private final Flag userFlag;
    private final Flag rootFlag;

    private Box(Long id, String slug, String name, OperatingSystem operatingSystem, Difficulty difficulty,
                String synopsis, String ipAddress, String maker, Instant releasedAt, boolean retired,
                Flag userFlag, Flag rootFlag) {
        this.id = id;
        this.slug = requireSlug(slug);
        this.name = requireName(name);
        this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        this.synopsis = Objects.requireNonNull(synopsis, "synopsis");
        this.ipAddress = Objects.requireNonNull(ipAddress, "ipAddress");
        this.maker = Objects.requireNonNull(maker, "maker");
        this.releasedAt = Objects.requireNonNull(releasedAt, "releasedAt");
        this.retired = retired;
        this.userFlag = Objects.requireNonNull(userFlag, "userFlag");
        this.rootFlag = Objects.requireNonNull(rootFlag, "rootFlag");
    }

    /** Nouvelle machine du catalogue, pas encore persistée. */
    public static Box create(String slug, String name, OperatingSystem operatingSystem, Difficulty difficulty,
                             String synopsis, String ipAddress, String maker, Instant releasedAt,
                             Flag userFlag, Flag rootFlag) {
        return new Box(null, slug, name, operatingSystem, difficulty, synopsis, ipAddress, maker, releasedAt,
                false, userFlag, rootFlag);
    }

    /** Reconstitution depuis la persistance. */
    public static Box restore(Long id, String slug, String name, OperatingSystem operatingSystem,
                              Difficulty difficulty, String synopsis, String ipAddress, String maker,
                              Instant releasedAt, boolean retired, Flag userFlag, Flag rootFlag) {
        return new Box(Objects.requireNonNull(id, "id"), slug, name, operatingSystem, difficulty, synopsis,
                ipAddress, maker, releasedAt, retired, userFlag, rootFlag);
    }

    /**
     * Transforme une soumission en possession, ou refuse la soumission.
     * Le flag n'est comparé qu'ici : aucune couche au-dessus ne le manipule.
     */
    public Own claim(Long userId, FlagKind kind, String submittedFlag, boolean firstBlood, Instant now) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(kind, "kind");
        if (!flagOf(kind).matches(submittedFlag)) {
            throw new InvalidInputException("Flag incorrect");
        }
        return Own.record(userId, id, kind, pointsFor(kind), firstBlood, now);
    }

    public int pointsFor(FlagKind kind) {
        return difficulty.pointsFor(kind);
    }

    /** Points d'une machine entièrement possédée : c'est ce qu'elle pèse dans la progression. */
    public int totalPoints() {
        return difficulty.totalPoints();
    }

    private Flag flagOf(FlagKind kind) {
        return kind == FlagKind.USER ? userFlag : rootFlag;
    }

    private static String requireSlug(String slug) {
        if (slug == null || !SLUG_FORMAT.matcher(slug).matches()) {
            throw new InvalidInputException("Identifiant de machine invalide");
        }
        return slug;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new InvalidInputException("Le nom de la machine est obligatoire");
        }
        return name.trim();
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMaker() {
        return maker;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public Flag getUserFlag() {
        return userFlag;
    }

    public Flag getRootFlag() {
        return rootFlag;
    }
}
