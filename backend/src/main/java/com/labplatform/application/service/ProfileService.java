package com.labplatform.application.service;

import com.labplatform.application.port.in.profile.ActivityEntry;
import com.labplatform.application.port.in.profile.GetAchievementsUseCase;
import com.labplatform.application.port.in.profile.GetActivityUseCase;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.application.port.out.SectionCompletionRepositoryPort;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.SectionCompletion;
import com.labplatform.domain.academy.Track;
import com.labplatform.domain.achievement.Achievement;
import com.labplatform.domain.achievement.PlayerRecord;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.user.Actor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Profil d'un joueur : hauts faits et activité récente.
 * <p>
 * Rien n'est stocké ici. Les deux se déduisent des flags validés et des
 * sections terminées, donc ils ne peuvent pas se désynchroniser de la
 * réalité, et le catalogue peut grandir sans migration.
 */
public class ProfileService implements GetAchievementsUseCase, GetActivityUseCase {

    private static final int MAX_ACTIVITY_SIZE = 100;

    private final BoxRepositoryPort boxes;
    private final OwnRepositoryPort owns;
    private final CourseRepositoryPort courses;
    private final SectionCompletionRepositoryPort completions;

    public ProfileService(BoxRepositoryPort boxes, OwnRepositoryPort owns, CourseRepositoryPort courses,
                          SectionCompletionRepositoryPort completions) {
        this.boxes = boxes;
        this.owns = owns;
        this.courses = courses;
        this.completions = completions;
    }

    @Override
    public List<Achievement.Earned> achievements(Actor actor) {
        return Achievement.evaluate(recordOf(actor));
    }

    @Override
    public List<ActivityEntry> activity(Actor actor, int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_ACTIVITY_SIZE);
        Map<Long, Box> boxById = boxes.findAll().stream().collect(Collectors.toMap(Box::getId, Function.identity()));
        List<Course> catalogue = courses.findAll();

        List<ActivityEntry> entries = new ArrayList<>();
        for (Own own : owns.findByUser(actor.userId())) {
            Box box = boxById.get(own.boxId());
            if (box == null) {
                continue;
            }
            entries.add(new ActivityEntry(ActivityEntry.Kind.FLAG, box.getName(),
                    own.kind() == FlagKind.USER ? "Flag utilisateur validé" : "Flag root validé",
                    own.points(), own.firstBlood(), own.ownedAt()));
        }
        for (SectionCompletion done : completions.findByUser(actor.userId())) {
            sectionOf(catalogue, done).ifPresent(found -> entries.add(new ActivityEntry(
                    ActivityEntry.Kind.SECTION, found.section().title(),
                    found.course().getTitle() + " · " + found.section().kind().displayName(),
                    0, false, done.completedAt())));
        }
        return entries.stream()
                .sorted(Comparator.comparing(ActivityEntry::at).reversed())
                .limit(size)
                .toList();
    }

    private PlayerRecord recordOf(Actor actor) {
        List<Own> mine = owns.findByUser(actor.userId());
        Map<Long, Box> boxById = boxes.findAll().stream().collect(Collectors.toMap(Box::getId, Function.identity()));

        Map<Long, Set<FlagKind>> kindsByBox = mine.stream()
                .collect(Collectors.groupingBy(Own::boxId, Collectors.mapping(Own::kind, Collectors.toSet())));
        List<Box> pwned = kindsByBox.entrySet().stream()
                .filter(entry -> entry.getValue().containsAll(Set.of(FlagKind.USER, FlagKind.ROOT)))
                .map(entry -> boxById.get(entry.getKey()))
                .filter(box -> box != null)
                .toList();
        Difficulty hardest = pwned.stream()
                .map(Box::getDifficulty)
                .max(Comparator.comparingInt(Difficulty::ordinal))
                .orElse(null);

        List<SectionCompletion> done = completions.findByUser(actor.userId());
        Set<Long> doneSectionIds = done.stream().map(SectionCompletion::sectionId).collect(Collectors.toSet());
        List<Course> catalogue = courses.findAll();
        int coursesCompleted = (int) catalogue.stream()
                .filter(course -> course.progressOf(doneSectionIds).isCompleted())
                .count();
        Set<Track> tracksStarted = new HashSet<>();
        catalogue.stream()
                .filter(course -> course.progressOf(doneSectionIds).isStarted())
                .forEach(course -> tracksStarted.add(course.getTrack()));

        return new PlayerRecord(mine.size(), pwned.size(), (int) mine.stream().filter(Own::firstBlood).count(),
                hardest, coursesCompleted, done.size(), tracksStarted.size());
    }

    private static Optional<CourseAndSection> sectionOf(List<Course> catalogue, SectionCompletion completion) {
        return catalogue.stream()
                .filter(course -> course.getId().equals(completion.courseId()))
                .flatMap(course -> course.getSections().stream()
                        .filter(section -> section.id().equals(completion.sectionId()))
                        .map(section -> new CourseAndSection(course, section)))
                .findFirst();
    }

    private record CourseAndSection(Course course, CourseSection section) {
    }
}
