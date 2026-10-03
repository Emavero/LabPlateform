package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.CourseAttackStageJpaEntity;
import com.labplatform.adapter.out.persistence.entity.CourseDesignerJpaEntity;
import com.labplatform.adapter.out.persistence.entity.CourseJpaEntity;
import com.labplatform.adapter.out.persistence.entity.CourseSectionJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataCourseAttackStageRepository;
import com.labplatform.adapter.out.persistence.repository.SpringDataCourseDesignerRepository;
import com.labplatform.adapter.out.persistence.repository.SpringDataCourseRepository;
import com.labplatform.adapter.out.persistence.repository.SpringDataCourseSectionRepository;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.domain.academy.AttackPath;
import com.labplatform.domain.academy.AttackStage;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseBriefing;
import com.labplatform.domain.academy.CourseDesigner;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.RealWorldCase;
import com.labplatform.domain.academy.Track;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Le cours, ses sections, les étapes de son chemin d'attaque et ses concepteurs
 * sont quatre tables mais un seul agrégat : elles sont rechargées ensemble, en
 * une requête par table plutôt qu'une par cours.
 */
@Component
public class CoursePersistenceAdapter implements CourseRepositoryPort {

    private final SpringDataCourseRepository courses;
    private final SpringDataCourseSectionRepository sections;
    private final SpringDataCourseAttackStageRepository stages;
    private final SpringDataCourseDesignerRepository designers;

    public CoursePersistenceAdapter(SpringDataCourseRepository courses, SpringDataCourseSectionRepository sections,
                                    SpringDataCourseAttackStageRepository stages,
                                    SpringDataCourseDesignerRepository designers) {
        this.courses = courses;
        this.sections = sections;
        this.stages = stages;
        this.designers = designers;
    }

    @Override
    public List<Course> findAll() {
        return assemble(courses.findAll());
    }

    @Override
    public List<Course> findByTrack(Track track) {
        return assemble(courses.findByTrack(track));
    }

    @Override
    public Optional<Course> findBySlug(String slug) {
        return courses.findBySlug(slug).map(entity -> assemble(List.of(entity)).get(0));
    }

    @Override
    public long count() {
        return courses.count();
    }

    @Override
    @Transactional
    public Course save(Course course) {
        AttackPath attack = course.getBriefing().attackPath();
        RealWorldCase realCase = course.getBriefing().realCase();
        CourseJpaEntity saved = courses.save(new CourseJpaEntity(course.getId(), course.getSlug(), course.getTitle(),
                course.getTrack(), course.getTopic(), course.getLevel(), course.getSummary(), course.getPublishedAt(),
                attack.summary(), realCase.sector(), realCase.situation(), realCase.stake(), realCase.outcome()));
        List<CourseSection> storedSections = course.getSections().stream()
                .map(section -> toDomain(sections.save(new CourseSectionJpaEntity(section.id(), saved.getId(),
                        section.slug(), section.title(), section.kind(), section.position(), section.minutes(),
                        section.content(), section.videoUrl()))))
                .toList();
        // Les sections retirées du cours disparaissent ; celles qui restent gardent
        // leur identifiant, donc l'avancement des apprenants survit à une refonte.
        Set<Long> kept = storedSections.stream().map(CourseSection::id).collect(Collectors.toSet());
        sections.findByCourseIdInOrderByPosition(List.of(saved.getId())).stream()
                .map(CourseSectionJpaEntity::getId)
                .filter(id -> !kept.contains(id))
                .forEach(sections::deleteById);

        // Étapes et concepteurs ne portent aucun avancement : les réécrire en bloc
        // est plus simple, et plus sûr, qu'un rapprochement ligne à ligne.
        stages.deleteByCourseId(saved.getId());
        designers.deleteByCourseId(saved.getId());
        List<AttackStage> storedStages = attack.stages().stream()
                .map(stage -> toDomain(stages.save(new CourseAttackStageJpaEntity(null, saved.getId(),
                        stage.position(), stage.name(), stage.description(), stage.technique()))))
                .toList();
        List<CourseDesigner> storedDesigners = course.getBriefing().designers().stream()
                .map(designer -> toDomain(designers.save(new CourseDesignerJpaEntity(null, saved.getId(),
                        designer.position(), designer.name(), designer.role(), designer.avatarUrl()))))
                .toList();

        return toDomain(saved, storedSections, storedStages, storedDesigners);
    }

    @Override
    @Transactional
    public void delete(Course course) {
        stages.deleteByCourseId(course.getId());
        designers.deleteByCourseId(course.getId());
        sections.deleteByCourseId(course.getId());
        courses.deleteById(course.getId());
    }

    @Override
    @Transactional
    public void deleteAll() {
        stages.deleteAllInBatch();
        designers.deleteAllInBatch();
        sections.deleteAllInBatch();
        courses.deleteAllInBatch();
    }

    private List<Course> assemble(List<CourseJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Collection<Long> ids = entities.stream().map(CourseJpaEntity::getId).toList();
        Map<Long, List<CourseSection>> sectionsByCourse = sections.findByCourseIdInOrderByPosition(ids).stream()
                .collect(Collectors.groupingBy(CourseSectionJpaEntity::getCourseId,
                        Collectors.mapping(CoursePersistenceAdapter::toDomain, Collectors.toList())));
        Map<Long, List<AttackStage>> stagesByCourse = stages.findByCourseIdInOrderByPosition(ids).stream()
                .collect(Collectors.groupingBy(CourseAttackStageJpaEntity::getCourseId,
                        Collectors.mapping(CoursePersistenceAdapter::toDomain, Collectors.toList())));
        Map<Long, List<CourseDesigner>> designersByCourse = designers.findByCourseIdInOrderByPosition(ids).stream()
                .collect(Collectors.groupingBy(CourseDesignerJpaEntity::getCourseId,
                        Collectors.mapping(CoursePersistenceAdapter::toDomain, Collectors.toList())));
        return entities.stream()
                .map(entity -> toDomain(entity, sectionsByCourse.getOrDefault(entity.getId(), List.of()),
                        stagesByCourse.getOrDefault(entity.getId(), List.of()),
                        designersByCourse.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    private static Course toDomain(CourseJpaEntity e, List<CourseSection> sections, List<AttackStage> stages,
                                   List<CourseDesigner> designers) {
        CourseBriefing briefing = new CourseBriefing(
                AttackPath.of(e.getAttackSummary(), stages),
                RealWorldCase.of(e.getCaseSector(), e.getCaseSituation(), e.getCaseStake(), e.getCaseOutcome()),
                designers);
        return Course.restore(e.getId(), e.getSlug(), e.getTitle(), e.getTopic(), e.getLevel(), e.getSummary(),
                e.getPublishedAt(), sections, briefing);
    }

    private static CourseSection toDomain(CourseSectionJpaEntity e) {
        return new CourseSection(e.getId(), e.getSlug(), e.getTitle(), e.getKind(), e.getPosition(), e.getMinutes(),
                e.getContent(), e.getVideoUrl());
    }

    private static AttackStage toDomain(CourseAttackStageJpaEntity e) {
        return AttackStage.of(e.getId(), e.getPosition(), e.getName(), e.getDescription(), e.getTechnique());
    }

    private static CourseDesigner toDomain(CourseDesignerJpaEntity e) {
        return CourseDesigner.of(e.getId(), e.getPosition(), e.getName(), e.getRole(), e.getAvatarUrl());
    }
}
