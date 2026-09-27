package com.labplatform.application.service;

import com.labplatform.application.port.in.billing.GetEffectivePlanUseCase;
import com.labplatform.application.port.in.scenario.ListScenariosUseCase;
import com.labplatform.application.port.in.scenario.ManageScenariosUseCase;
import com.labplatform.application.port.in.scenario.ScenarioDraft;
import com.labplatform.application.port.in.scenario.ScenarioView;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.application.port.out.ScenarioRepositoryPort;
import com.labplatform.application.port.out.SectionCompletionRepositoryPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.billing.ProAccessPolicy;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.scenario.Scenario;
import com.labplatform.domain.scenario.ScenarioEvidence;
import com.labplatform.domain.scenario.ScenarioProgress;
import com.labplatform.domain.scenario.ScenarioStep;
import com.labplatform.domain.scenario.ScenarioStepKind;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Scénarios d'exercice.
 * <p>
 * Le service ne tient aucun avancement : il rassemble ce que le joueur a obtenu
 * — flags validés, cours terminés — et laisse l'agrégat en déduire où il en est.
 * Un scénario suivi n'a donc rien à démarrer ni à abandonner, et il reste juste
 * même si le joueur a fait le travail avant de l'ouvrir.
 * <p>
 * Les références des étapes sont vérifiées à l'enregistrement : publier un
 * scénario qui désigne une machine inexistante donnerait une étape
 * infranchissable, et le joueur en conclurait que la plateforme est cassée.
 * Une ressource retirée <em>après</em> la publication est signalée comme
 * manquante plutôt que masquée — c'est à l'administration de trancher.
 */
public class ScenarioService implements ListScenariosUseCase, ManageScenariosUseCase {

    private final ScenarioRepositoryPort scenarios;
    private final BoxRepositoryPort boxes;
    private final CourseRepositoryPort courses;
    private final OwnRepositoryPort owns;
    private final SectionCompletionRepositoryPort completions;
    private final GetEffectivePlanUseCase plans;
    private final TransactionPort transactions;
    private final Clock clock;

    public ScenarioService(ScenarioRepositoryPort scenarios, BoxRepositoryPort boxes, CourseRepositoryPort courses,
                           OwnRepositoryPort owns, SectionCompletionRepositoryPort completions,
                           GetEffectivePlanUseCase plans, TransactionPort transactions, Clock clock) {
        this.scenarios = scenarios;
        this.boxes = boxes;
        this.courses = courses;
        this.owns = owns;
        this.completions = completions;
        this.plans = plans;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public List<ScenarioView> listPublished(Actor actor) {
        Context context = context(actor);
        return scenarios.findPublished().stream().map(scenario -> view(scenario, context)).toList();
    }

    @Override
    public ScenarioView get(Actor actor, String slug) {
        Scenario scenario = require(slug);
        if (!scenario.isPublished() && !actor.isAdmin()) {
            // Introuvable et non « interdit » : un brouillon ne s'annonce pas.
            throw new NotFoundException("Scénario introuvable");
        }
        return view(scenario, context(actor));
    }

    @Override
    public List<ScenarioView> listAll(Actor actor) {
        requireAdmin(actor);
        Context context = context(actor);
        return scenarios.findAll().stream().map(scenario -> view(scenario, context)).toList();
    }

    @Override
    public Scenario create(Actor actor, ScenarioDraft draft) {
        requireAdmin(actor);
        List<ScenarioStep> steps = steps(draft);
        return transactions.inTransaction(
                () -> scenarios.save(Scenario.compose(draft.title(), draft.brief(), steps, draft.published(),
                        clock.instant())));
    }

    @Override
    public Scenario update(Actor actor, String slug, ScenarioDraft draft) {
        requireAdmin(actor);
        Scenario scenario = require(slug);
        List<ScenarioStep> steps = steps(draft);
        return transactions.inTransaction(() -> {
            scenario.revise(draft.title(), draft.brief(), steps, draft.published(), clock.instant());
            return scenarios.save(scenario);
        });
    }

    @Override
    public void delete(Actor actor, String slug) {
        requireAdmin(actor);
        Scenario scenario = require(slug);
        transactions.inTransaction(() -> scenarios.delete(scenario));
    }

    /**
     * Étapes du brouillon, position donnée par l'ordre de la liste et références
     * vérifiées contre le catalogue.
     */
    private List<ScenarioStep> steps(ScenarioDraft draft) {
        if (draft.steps() == null) {
            return List.of();
        }
        Set<String> knownBoxes = new HashSet<>();
        boxes.findAll().forEach(box -> knownBoxes.add(box.getSlug()));
        Set<String> knownCourses = new HashSet<>();
        courses.findAll().forEach(course -> knownCourses.add(course.getSlug()));

        List<ScenarioStep> steps = new ArrayList<>();
        int position = 1;
        for (ScenarioDraft.StepDraft step : draft.steps()) {
            String reference = step.reference() == null ? "" : step.reference().trim().toLowerCase(Locale.ROOT);
            Set<String> known = step.kind() == ScenarioStepKind.MACHINE ? knownBoxes : knownCourses;
            if (!known.contains(reference)) {
                throw new InvalidInputException(step.kind() == ScenarioStepKind.MACHINE
                        ? "Machine introuvable : " + reference
                        : "Cours introuvable : " + reference);
            }
            steps.add(ScenarioStep.drafted(position++, step.kind(), reference, step.instruction(), step.objective()));
        }
        return steps;
    }

    /** Ce qu'il faut savoir du lecteur et du catalogue pour présenter un scénario. */
    private record Context(ScenarioEvidence evidence, Map<String, Box> boxes, Map<String, Course> courses,
                           boolean unlocked) {
    }

    private Context context(Actor actor) {
        Map<String, Box> boxesBySlug = new HashMap<>();
        Map<Long, String> slugById = new HashMap<>();
        boxes.findAll().forEach(box -> {
            boxesBySlug.put(box.getSlug(), box);
            slugById.put(box.getId(), box.getSlug());
        });
        Map<String, Course> coursesBySlug = new HashMap<>();
        Map<Long, Course> coursesById = new HashMap<>();
        courses.findAll().forEach(course -> {
            coursesBySlug.put(course.getSlug(), course);
            coursesById.put(course.getId(), course);
        });

        Map<String, Set<FlagKind>> flags = new HashMap<>();
        for (Own own : owns.findByUser(actor.userId())) {
            String slug = slugById.get(own.boxId());
            if (slug != null) {
                flags.computeIfAbsent(slug, key -> new HashSet<>()).add(own.kind());
            }
        }

        // Un cours compte comme terminé quand toutes ses sections le sont : une
        // étape « suivre ce cours » ne se valide pas à la première page lue.
        Map<Long, Set<Long>> doneSections = new HashMap<>();
        completions.findByUser(actor.userId()).forEach(completion -> doneSections
                .computeIfAbsent(completion.courseId(), key -> new HashSet<>())
                .add(completion.sectionId()));
        Set<String> finished = new HashSet<>();
        coursesById.forEach((courseId, course) -> {
            Set<Long> done = doneSections.getOrDefault(courseId, Set.of());
            if (!course.getSections().isEmpty() && course.progressOf(done).isCompleted()) {
                finished.add(course.getSlug());
            }
        });

        return new Context(new ScenarioEvidence(flags, finished), boxesBySlug, coursesBySlug,
                ProAccessPolicy.granted(actor, plans.planOf(actor)));
    }

    private ScenarioView view(Scenario scenario, Context context) {
        ScenarioProgress progress = scenario.progressOf(context.evidence());
        Set<Long> done = new HashSet<>(progress.doneStepIds());
        List<ScenarioView.ScenarioStepView> steps = scenario.getSteps().stream()
                .map(step -> stepView(step, context, done.contains(step.id())))
                .toList();
        return new ScenarioView(scenario, steps, progress);
    }

    private ScenarioView.ScenarioStepView stepView(ScenarioStep step, Context context, boolean done) {
        if (step.kind() == ScenarioStepKind.MACHINE) {
            Box box = context.boxes().get(step.reference());
            boolean locked = box != null && box.isProOnly() && !context.unlocked();
            return new ScenarioView.ScenarioStepView(step, box == null ? step.reference() : box.getName(), box == null,
                    locked, done);
        }
        Course course = context.courses().get(step.reference());
        return new ScenarioView.ScenarioStepView(step, course == null ? step.reference() : course.getTitle(),
                course == null, false, done);
    }

    private Scenario require(String slug) {
        return scenarios.findBySlug(slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Scénario introuvable"));
    }

    private static void requireAdmin(Actor actor) {
        if (!actor.isAdmin()) {
            throw new ForbiddenException("Réservé à l'administration");
        }
    }
}
