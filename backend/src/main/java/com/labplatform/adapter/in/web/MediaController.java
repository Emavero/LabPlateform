package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.media.GetMediaUseCase;
import com.labplatform.domain.media.MediaAsset;
import jakarta.servlet.http.HttpServletRequest;
import com.labplatform.domain.shared.NotFoundException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Sert les fichiers téléversés, aux seuls utilisateurs connectés.
 * <p>
 * Les requêtes par plage (en-tête Range) sont honorées : sans elles, un
 * navigateur ne sait pas se déplacer dans une vidéo sans la retélécharger.
 * Le type renvoyé est celui validé au téléversement, jamais celui deviné
 * depuis le nom du fichier, et « nosniff » interdit au navigateur d'en
 * choisir un autre.
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    /** Taille d'une plage servie par défaut : de quoi démarrer sans tout envoyer. */
    private static final long CHUNK_SIZE = 2L * 1024 * 1024;

    private final GetMediaUseCase getMedia;

    public MediaController(GetMediaUseCase getMedia) {
        this.getMedia = getMedia;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> stream(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                                    HttpServletRequest request) {
        GetMediaUseCase.Media media = getMedia.get(user.toActor(), id);
        MediaAsset asset = media.asset();
        Resource file = new FileSystemResource(media.content().path());
        MediaType type = MediaType.parseMediaType(asset.contentType());

        List<HttpRange> ranges = HttpRange.parseRanges(request.getHeader(HttpHeaders.RANGE));
        if (ranges.isEmpty()) {
            return ResponseEntity.ok()
                    .headers(headers(asset))
                    .contentType(type)
                    .contentLength(media.content().sizeBytes())
                    .body(file);
        }
        long size = media.content().sizeBytes();
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(size);
        // La plage servie est bornée : un navigateur qui demande tout le fichier
        // le reçoit en plusieurs morceaux plutôt qu'en une seule lecture mémoire.
        long end = Math.min(range.getRangeEnd(size), start + CHUNK_SIZE - 1);
        byte[] slice = slice(media.content().path(), start, end - start + 1);

        HttpHeaders headers = headers(asset);
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + size);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .contentType(type)
                .contentLength(slice.length)
                .body(new ByteArrayResource(slice));
    }

    private static byte[] slice(String path, long start, long length) {
        try (InputStream stream = Files.newInputStream(Path.of(path))) {
            stream.skipNBytes(start);
            return stream.readNBytes((int) length);
        } catch (IOException e) {
            throw new NotFoundException("Fichier introuvable");
        }
    }

    private static HttpHeaders headers(MediaAsset asset) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set("X-Content-Type-Options", "nosniff");
        // Affiché dans la page, jamais exécuté : le nom ne sert qu'au téléchargement.
        headers.setContentDisposition(org.springframework.http.ContentDisposition.inline()
                .filename(asset.filename())
                .build());
        headers.setCacheControl("private, max-age=3600");
        return headers;
    }
}
