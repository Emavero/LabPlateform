package com.labplatform.domain.academy;

import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cours d'une filière, découpé en sections. L'agrégat possède ses sections :
 * c'est lui qui les ordonne, qui répond de leur existence et qui calcule
 * l'avancement d'un apprenant.
 * <p>
 * La filière n'est pas un champ : elle se déduit du sous-domaine, qui la
 * connaît. Un cours ne peut donc pas se contredire — pas de « Analyse mémoire »
 * rangé en Défense.
 */
public class Course {

    private static final Pattern SLUG_FORMAT = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private final Long id;
    private final String slug;
    private final String title;
    private final CourseTopic topic;
    private final CourseLevel level;
    private final String summary;
    private final Instant publishedAt;
    private final List<CourseSection> sections;
    private final CourseBriefing briefing;

    private Course(Long id, String slug, String title, CourseTopic topic, CourseLevel level, String summary,
                   Instant publishedAt, List<CourseSection> sections, CourseBriefing briefing) {
        this.id = id;
        this.slug = requireSlug(slug);
        this.title = Objects.requireNonNull(title, "title");
        this.topic = Objects.requireNonNull(topic, "topic");
        this.level = Objects.requireNonNull(level, "level");
        this.summary = Objects.requireNonNull(summary, "summary");
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt");
        this.briefing = briefing == null ? CourseBriefing.empty() : briefing;
        this.sections = Objects.requireNonNull(sections, "sections").stream()
                .sorted(Comparator.comparingInt(CourseSection::position))
                .toList();
        if (this.sections.isEmpty()) {
            throw new InvalidInputException("Un cours comporte au moins une section");
        }
    }

    public static Course create(String slug, String title, CourseTopic topic, CourseLevel level, String summary,
                                Instant publishedAt, List<CourseSection> sections) {
        return create(slug, title, topic, level, summary, publishedAt, sections, CourseBriefing.empty());
    }

    public static Course create(String slug, String title, CourseTopic topic, CourseLevel level, String summary,
                                Instant publishedAt, List<CourseSection> sections, CourseBriefing briefing) {
        return new Course(null, slug, title, topic, level, summary, publishedAt, sections, briefing);
    }

    public static Course restore(Long id, String slug, String title, CourseTopic topic, CourseLevel level,
                                 String summary, Instant publishedAt, List<CourseSection> sections) {
        return restore(id, slug, title, topic, level, summary, publishedAt, sections, CourseBriefing.empty());
    }

    public static Course restore(Long id, String slug, String title, CourseTopic topic, CourseLevel level,
                                 String summary, Instant publishedAt, List<CourseSection> sections,
                                 CourseBriefing briefing) {
        return new Course(Objects.requireNonNull(id, "id"), slug, title, topic, level, summary, publishedAt, sections,
                briefing);
    }

    /** Section de ce cours, ou 404 : une section d'un autre cours n'existe pas ici. */
    public CourseSection requireSection(String sectionSlug) {
        return sections.stream()
                .filter(section -> section.slug().equals(sectionSlug))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Section introuvable"));
    }

    /** Avancement d'un apprenant, à partir des seules sections qu'il a cochées. */
    public CourseProgress progressOf(Set<Long> completedSectionIds) {
        int done = (int) sections.stream().filter(section -> completedSectionIds.contains(section.id())).count();
        return new CourseProgress(done, sections.size());
    }

    public int totalMinutes() {
        return sections.stream().mapToInt(CourseSection::minutes).sum();
    }

    private static String requireSlug(String slug) {
        if (slug == null || !SLUG_FORMAT.matcher(slug).matches()) {
            throw new InvalidInputException("Identifiant de cours invalide");
        }
        return slug;
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

    /** Filière, telle que le sous-domaine la désigne. */
    public Track getTrack() {
        return topic.track();
    }

    public CourseTopic getTopic() {
        return topic;
    }

    public CourseBriefing getBriefing() {
        return briefing;
    }

    public CourseLevel getLevel() {
        return level;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public List<CourseSection> getSections() {
        return sections;
    }
}
