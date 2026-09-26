package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.media.UploadMediaUseCase;
import com.labplatform.domain.media.MediaAsset;
import com.labplatform.domain.shared.InvalidInputException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

/** Téléversement des vidéos de cours. Rôle ADMIN exigé (voir SecurityConfig). */
@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {

    private final UploadMediaUseCase uploadMedia;

    public AdminMediaController(UploadMediaUseCase uploadMedia) {
        this.uploadMedia = uploadMedia;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse upload(@AuthenticationPrincipal AuthenticatedUser user,
                                @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidInputException("Le fichier est vide");
        }
        try (var content = file.getInputStream()) {
            return MediaResponse.from(uploadMedia.upload(user.toActor(), file.getOriginalFilename(),
                    file.getContentType(), file.getSize(), content));
        } catch (IOException e) {
            throw new InvalidInputException("Le fichier n'a pas pu être lu");
        }
    }

    /** L'adresse renvoyée est celle à coller dans la section de cours. */
    public record MediaResponse(String id, String url, String filename, String contentType, long sizeBytes,
                                Instant uploadedAt) {

        static MediaResponse from(MediaAsset asset) {
            return new MediaResponse(asset.id(), asset.url(), asset.filename(), asset.contentType(),
                    asset.sizeBytes(), asset.uploadedAt());
        }
    }
}
