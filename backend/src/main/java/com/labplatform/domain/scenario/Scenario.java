package com.labplatform.domain.scenario;

import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.Slug;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Scénario d'exercice : une situation, et les étapes à franchir dans l'ordre.
 * <p>
 * Un scénario n'ajoute aucun contenu : il ordonne ce qui existe déjà — des
 * machines du catalogue, des cours de l'académie. C'est le choix central du
 * module : l'avancement n'a donc rien à stocker, il se déduit des flags validés
 * et des sections terminées. Un joueur qui a possédé une machine avant de
 * commencer le scénario en voit l'étape déjà franchie, ce qui est la seule
 * lecture honnête de son travail.
 * <p>
 * Le lien ({@code slug}) est dérivé du titre une fois pour toutes : renommer un
 * scénario ne casse pas les liens déjà partagés.
 */
public class Scenario {

    private static final int MAX_TITLE_LENGTH = 140;
    private static final int MAX_BRIEF_LENGTH = 2_000;
    private static final int MAX_STEPS = 20;

    private final Long id;
    private final String slug;
    private String title;
    private String brief;
    private boolean published;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<ScenarioStep> steps;

    private Scenario(Long id, String slug, String title, String brief, boolean published, Instant createdAt,
                     Instant updatedAt, List<ScenarioStep> steps) {
        this.id = id;
        this.slug = Objects.requireNonNull(slug, "slug");
        this.title = requireTitle(title);
        this.brief = requireBrief(brief);
        this.published = published;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.steps = ordered(steps);
    }

    public static Scenario compose(String title, String brief, List<ScenarioStep> steps, boolean published,
                                   Instant now) {
        return new Scenario(null, Slug.from(title), title, brief, published, now, now, requireSteps(steps, published));
    }

    public static Scenario restore(Long id, String slug, String title, String brief, boolean published,
                                   Instant createdAt, Instant updatedAt, List<ScenarioStep> steps) {
        return new Scenario(Objects.requireNonNull(id, "id"), slug, title, brief, published, createdAt, updatedAt,
                steps);
    }

    /** Réécriture par l'administration. Le lien ne change pas, le titre peut. */
    public void revise(String title, String brief, List<ScenarioStep> steps, boolean published, Instant now) {
        this.title = requireTitle(title);
        this.brief = requireBrief(brief);
        this.published = published;
        this.steps.clear();
        this.steps.addAll(ordered(requireSteps(steps, published)));
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    /**
     * Avancement d'un joueur sur ce scénario.
     *
     * @param evidence ce que le joueur a déjà obtenu : l'appelant le lui demande
     *                 une fois, et l'agrégat n'interroge rien lui-même
     */
    public ScenarioProgress progressOf(ScenarioEvidence evidence) {
        List<Long> done = new ArrayList<>();
        Integer next = null;
        for (ScenarioStep step : steps) {
            if (evidence.satisfies(step)) {
                done.add(step.id());
            } else if (next == null) {
                // La première étape non franchie est celle à faire : un scénario
                // se suit dans l'ordre, même si rien n'empêche de sauter.
                next = step.position();
            }
        }
        return new ScenarioProgress(done.size(), steps.size(), next, done);
    }

    private static List<ScenarioStep> requireSteps(List<ScenarioStep> steps, boolean published) {
        if (steps == null || steps.isEmpty()) {
            // Un brouillon peut être vide, un scénario publié non : il n'y aurait
            // rien à faire, et il compterait pour terminé dès son ouverture.
            if (published) {
                throw new ConflictException("Un scénario publié comporte au moins une étape");
            }
            return List.of();
        }
        if (steps.size() > MAX_STEPS) {
            throw new InvalidInputException("Un scénario comporte au plus " + MAX_STEPS + " étapes");
        }
        return steps;
    }

    /** Les étapes se suivent dans l'ordre de leur position, quelle que soit celle de la saisie. */
    private static List<ScenarioStep> ordered(List<ScenarioStep> steps) {
        List<ScenarioStep> sorted = new ArrayList<>(Objects.requireNonNull(steps, "steps"));
        sorted.sort(java.util.Comparator.comparingInt(ScenarioStep::position));
        return sorted;
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidInputException("Le titre est obligatoire");
        }
        String trimmed = title.strip();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new InvalidInputException("Le titre est limité à " + MAX_TITLE_LENGTH + " caractères");
        }
        return trimmed;
    }

    private static String requireBrief(String brief) {
        if (brief == null || brief.isBlank()) {
            throw new InvalidInputException("La mise en situation est obligatoire");
        }
        String trimmed = brief.strip();
        if (trimmed.length() > MAX_BRIEF_LENGTH) {
            throw new InvalidInputException("La mise en situation est limitée à " + MAX_BRIEF_LENGTH + " caractères");
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getBrief() {
        return brief;
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

    public List<ScenarioStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /** Durée annoncée : la somme n'a de sens qu'avec les cours, elle reste au service. */
    public int getStepCount() {
        return steps.size();
    }
}
