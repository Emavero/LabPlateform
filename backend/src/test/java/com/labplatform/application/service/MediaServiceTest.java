package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryMedia;
import com.labplatform.domain.media.MediaAsset;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaServiceTest {

    private static final Actor ADMIN = new Actor(1L, Role.ADMIN);
    private static final Actor LEARNER = new Actor(2L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");
    private static final String ID = "0123456789abcdef0123456789abcdef";

    private InMemoryMedia media;
    private MediaService service;

    @BeforeEach
    void setUp() {
        media = new InMemoryMedia();
        // 20 octets au plus : de quoi éprouver la borne sans fabriquer un gros fichier.
        service = new MediaService(media, media, Fakes.secretGenerator(() -> "inutilisé", () -> ID),
                Fakes.NO_TRANSACTION, Clock.fixed(NOW, ZoneOffset.UTC), 20);
    }

    private static ByteArrayInputStream content(int bytes) {
        return new ByteArrayInputStream("x".repeat(bytes).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void uploadingReturnsAnAddressThatOnlyCarriesTheIdentifier() {
        MediaAsset asset = service.upload(ADMIN, "intro.mp4", "video/mp4", 10, content(10));

        assertEquals(ID, asset.id());
        assertEquals("/api/media/" + ID, asset.url());
        assertEquals(10, asset.sizeBytes());
        assertTrue(media.holds(ID));
    }

    @Test
    void theFilenameIsKeptForDisplayButNeverAsAPath() {
        MediaAsset asset = service.upload(ADMIN, "../../etc/passwd.mp4", "video/mp4", 5, content(5));

        assertFalse(asset.filename().contains("/"));
        assertFalse(asset.url().contains(".."));
    }

    @Test
    void onlyVideoFormatsAreAccepted() {
        assertThrows(InvalidInputException.class,
                () -> service.upload(ADMIN, "page.html", "text/html", 5, content(5)));
        assertThrows(InvalidInputException.class,
                () -> service.upload(ADMIN, "image.png", "image/png", 5, content(5)));
        // Le paramètre de jeu de caractères n'empêche pas la reconnaissance.
        assertEquals("video/mp4", service.upload(ADMIN, "ok.mp4", "video/mp4; charset=binary", 5, content(5))
                .contentType());
    }

    @Test
    void aFileLargerThanAllowedIsRefusedAndLeavesNothingBehind() {
        assertThrows(InvalidInputException.class, () -> service.upload(ADMIN, "gros.mp4", "video/mp4", 5,
                content(50)));

        // La taille annoncée mentait : c'est ce qui a été écrit qui compte, et il est effacé.
        assertFalse(media.holds(ID));
        assertEquals(0, media.count());
    }

    @Test
    void anEmptyFileIsRefused() {
        assertThrows(InvalidInputException.class, () -> service.upload(ADMIN, "vide.mp4", "video/mp4", 0,
                content(0)));
    }

    @Test
    void onlyAnAdministratorUploads() {
        assertThrows(ForbiddenException.class,
                () -> service.upload(LEARNER, "intro.mp4", "video/mp4", 5, content(5)));
    }

    @Test
    void anyLoggedInLearnerCanReadAnUploadedVideo() {
        service.upload(ADMIN, "intro.mp4", "video/mp4", 5, content(5));

        assertEquals(5, service.get(LEARNER, ID).content().sizeBytes());
        assertThrows(NotFoundException.class, () -> service.get(LEARNER, "f".repeat(32)));
    }
}
