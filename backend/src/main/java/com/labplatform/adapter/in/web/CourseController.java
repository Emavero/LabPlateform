package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.CourseDtos.CourseResponse;
import com.labplatform.adapter.in.web.dto.CourseDtos.CourseSummaryResponse;
import com.labplatform.adapter.in.web.dto.CourseDtos.LearningProgressResponse;
import com.labplatform.adapter.in.web.dto.CourseDtos.TrackResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.academy.GetCourseUseCase;
import com.labplatform.application.port.in.academy.GetLearningProgressUseCase;
import com.labplatform.application.port.in.academy.ListCoursesUseCase;
import com.labplatform.application.port.in.academy.TrackSectionProgressUseCase;
import com.labplatform.domain.academy.Track;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final ListCoursesUseCase listCourses;
    private final GetCourseUseCase getCourse;
    private final TrackSectionProgressUseCase trackProgress;
    private final GetLearningProgressUseCase learningProgress;

    public CourseController(ListCoursesUseCase listCourses, GetCourseUseCase getCourse,
                            TrackSectionProgressUseCase trackProgress,
                            GetLearningProgressUseCase learningProgress) {
        this.listCourses = listCourses;
        this.getCourse = getCourse;
        this.trackProgress = trackProgress;
        this.learningProgress = learningProgress;
    }

    /** Filières disponibles : c'est cette liste qui alimente le menu. */
    @GetMapping("/tracks")
    public List<TrackResponse> tracks() {
        return TrackResponse.all();
    }

    @GetMapping("/progress")
    public List<LearningProgressResponse> progress(@AuthenticationPrincipal AuthenticatedUser user) {
        return learningProgress.learningProgress(user.toActor()).stream()
                .map(LearningProgressResponse::from)
                .toList();
    }

    /** Catalogue, filtré par filière si {@code track} est fourni (son slug). */
    @GetMapping
    public List<CourseSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                            @RequestParam(required = false) String track) {
        Track filter = track == null || track.isBlank() ? null : Track.ofSlug(track);
        return listCourses.listCourses(user.toActor(), filter).stream().map(CourseSummaryResponse::from).toList();
    }

    @GetMapping("/{slug}")
    public CourseResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        return CourseResponse.from(getCourse.getCourse(user.toActor(), slug));
    }

    @PostMapping("/{slug}/sections/{sectionSlug}/completion")
    public CourseResponse complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug,
                                   @PathVariable String sectionSlug) {
        return CourseResponse.from(trackProgress.completeSection(user.toActor(), slug, sectionSlug));
    }

    @DeleteMapping("/{slug}/sections/{sectionSlug}/completion")
    public CourseResponse reopen(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug,
                                 @PathVariable String sectionSlug) {
        return CourseResponse.from(trackProgress.reopenSection(user.toActor(), slug, sectionSlug));
    }
}
