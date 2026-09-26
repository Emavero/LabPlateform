package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.AdminDtos.AdminOverviewResponse;
import com.labplatform.adapter.in.web.dto.AdminDtos.CourseDraftRequest;
import com.labplatform.adapter.in.web.dto.CourseDtos.AdminCourseResponse;
import com.labplatform.adapter.in.web.dto.CourseDtos.CourseResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.academy.GetCourseUseCase;
import com.labplatform.application.port.in.admin.GetAdminOverviewUseCase;
import com.labplatform.application.port.in.admin.ManageCoursesUseCase;
import com.labplatform.domain.academy.Course;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administration des cours. Tout /api/admin/** exige déjà le rôle ADMIN dans
 * la chaîne de sécurité ; les cas d'usage le revérifient de leur côté.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminCourseController {

    private final ManageCoursesUseCase manageCourses;
    private final GetAdminOverviewUseCase overview;
    private final GetCourseUseCase getCourse;

    public AdminCourseController(ManageCoursesUseCase manageCourses, GetAdminOverviewUseCase overview,
                                 GetCourseUseCase getCourse) {
        this.manageCourses = manageCourses;
        this.overview = overview;
        this.getCourse = getCourse;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview(@AuthenticationPrincipal AuthenticatedUser user) {
        return AdminOverviewResponse.from(overview.overview(user.toActor()));
    }

    /** Fiche complète d'un cours, bonnes réponses comprises : pour l'éditeur. */
    @GetMapping("/courses/{slug}")
    public CourseResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        return AdminCourseResponse.from(getCourse.getCourse(user.toActor(), slug));
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                 @Valid @RequestBody CourseDraftRequest request) {
        Course created = manageCourses.createCourse(user.toActor(), request.toDraft());
        return AdminCourseResponse.from(getCourse.getCourse(user.toActor(), created.getSlug()));
    }

    @PutMapping("/courses/{slug}")
    public CourseResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug,
                                 @Valid @RequestBody CourseDraftRequest request) {
        manageCourses.updateCourse(user.toActor(), slug, request.toDraft());
        return AdminCourseResponse.from(getCourse.getCourse(user.toActor(), slug));
    }

    @DeleteMapping("/courses/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        manageCourses.deleteCourse(user.toActor(), slug);
    }
}
