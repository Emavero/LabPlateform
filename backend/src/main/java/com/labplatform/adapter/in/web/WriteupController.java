package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.writeup.ListWriteupsUseCase;
import com.labplatform.application.port.in.writeup.WriteWriteupUseCase;
import com.labplatform.application.port.in.writeup.WriteupView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Comptes rendus d'une machine, écrits et lus par les joueurs qui l'ont possédée. */
@RestController
@RequestMapping("/api/boxes/{slug}/writeups")
public class WriteupController {

    private final ListWriteupsUseCase listWriteups;
    private final WriteWriteupUseCase writeWriteup;

    public WriteupController(ListWriteupsUseCase listWriteups, WriteWriteupUseCase writeWriteup) {
        this.listWriteups = listWriteups;
        this.writeWriteup = writeWriteup;
    }

    @GetMapping
    public List<WriteupResponse> list(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        return listWriteups.listWriteups(user.toActor(), slug).stream().map(WriteupResponse::from).toList();
    }

    /** Enregistre le compte rendu de l'appelant : un seul par machine. */
    @PutMapping("/mine")
    public WriteupResponse save(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug,
                                @Valid @RequestBody WriteupRequest request) {
        return WriteupResponse.from(writeWriteup.save(user.toActor(), slug, request.title(), request.content(),
                request.published()));
    }

    @DeleteMapping("/mine")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        writeWriteup.delete(user.toActor(), slug);
    }

    public record WriteupRequest(
            @NotBlank(message = "Le titre est obligatoire")
            @Size(max = 128, message = "Le titre est limité à 128 caractères") String title,
            @NotBlank(message = "Le compte rendu est vide")
            @Size(max = 40_000, message = "Le compte rendu est limité à 40 000 caractères") String content,
            boolean published) {
    }

    /** L'auteur n'apparaît que par son pseudonyme, jamais par son adresse e-mail. */
    public record WriteupResponse(String handle, boolean mine, String title, String content, boolean published,
                                  Instant createdAt, Instant updatedAt) {

        static WriteupResponse from(WriteupView view) {
            return new WriteupResponse(view.handle(), view.mine(), view.writeup().getTitle(),
                    view.writeup().getContent(), view.writeup().isPublished(), view.writeup().getCreatedAt(),
                    view.writeup().getUpdatedAt());
        }
    }
}
