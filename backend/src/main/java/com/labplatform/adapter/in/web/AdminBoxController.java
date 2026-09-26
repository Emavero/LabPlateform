package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.AdminDtos.AdminBoxResponse;
import com.labplatform.adapter.in.web.dto.AdminDtos.BoxDraftRequest;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.admin.ManageBoxesUseCase;
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

import java.util.List;

/** Administration du catalogue de machines. Rôle ADMIN exigé (voir SecurityConfig). */
@RestController
@RequestMapping("/api/admin/boxes")
public class AdminBoxController {

    private final ManageBoxesUseCase manageBoxes;

    public AdminBoxController(ManageBoxesUseCase manageBoxes) {
        this.manageBoxes = manageBoxes;
    }

    @GetMapping
    public List<AdminBoxResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return manageBoxes.listBoxes(user.toActor()).stream().map(AdminBoxResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminBoxResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody BoxDraftRequest request) {
        return AdminBoxResponse.from(manageBoxes.createBox(user.toActor(), request.toDraft()));
    }

    @PutMapping("/{slug}")
    public AdminBoxResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug,
                                   @Valid @RequestBody BoxDraftRequest request) {
        return AdminBoxResponse.from(manageBoxes.updateBox(user.toActor(), slug, request.toDraft()));
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        manageBoxes.deleteBox(user.toActor(), slug);
    }
}
