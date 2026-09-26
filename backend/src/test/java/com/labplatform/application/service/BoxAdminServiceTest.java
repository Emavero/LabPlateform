package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.port.in.admin.BoxDraft;
import com.labplatform.application.port.in.admin.PublishedBox;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxAdminServiceTest {

    private static final Actor ADMIN = new Actor(1L, Role.ADMIN);
    private static final Actor PLAYER = new Actor(2L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");
    private static final String FLAG = "abababababababababababababababab";

    private InMemoryBoxes boxes;
    private BoxAdminService admin;

    @BeforeEach
    void setUp() {
        boxes = new InMemoryBoxes();
        admin = new BoxAdminService(boxes, Fakes.secretGenerator(() -> "inutilisé"),
                Fakes.NO_TRANSACTION, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static BoxDraft draft(String name, String userFlag, String rootFlag) {
        return new BoxDraft(name, OperatingSystem.LINUX, Difficulty.HARD, "Synopsis.", "10.10.10.20", "cyberMans",
                false, userFlag, rootFlag);
    }

    @Test
    void publishingAMachineDerivesItsUrlAndTiresItsFlags() {
        PublishedBox published = admin.createBox(ADMIN, draft("Northwind Bis", null, null));

        Box box = published.box();
        assertEquals("northwind-bis", box.getSlug());
        assertEquals(Difficulty.HARD, box.getDifficulty());
        assertEquals(16, box.pointsFor(FlagKind.USER));
        // Les flags tirés sont renvoyés une fois, pour être déposés sur la cible.
        assertNotNull(published.userFlagOnce());
        assertNotNull(published.rootFlagOnce());
        assertTrue(box.getUserFlag().matches(published.userFlagOnce()));
    }

    @Test
    void anAdministratorCanImposeTheFlagsAlreadyPlantedOnTheTarget() {
        PublishedBox published = admin.createBox(ADMIN, draft("Cobalt Bis", FLAG, FLAG));

        assertTrue(published.box().getUserFlag().matches(FLAG));
        assertEquals(FLAG, published.userFlagOnce());
    }

    @Test
    void anEmptyFlagOnUpdateMeansUnchanged() {
        Box created = admin.createBox(ADMIN, draft("Mirage Bis", FLAG, FLAG)).box();

        PublishedBox updated = admin.updateBox(ADMIN, created.getSlug(),
                new BoxDraft("Mirage Bis", OperatingSystem.WINDOWS, Difficulty.INSANE, "Autre synopsis.",
                        "10.10.10.21", "cyberMans", true, "  ", null));

        assertTrue(updated.box().getUserFlag().matches(FLAG));
        assertNull(updated.userFlagOnce());
        assertEquals(OperatingSystem.WINDOWS, updated.box().getOperatingSystem());
        assertTrue(updated.box().isRetired());
        // Le lien ne change pas, même si le reste change.
        assertEquals("mirage-bis", updated.box().getSlug());
    }

    @Test
    void aMalformedFlagIsRefused() {
        assertThrows(InvalidInputException.class, () -> admin.createBox(ADMIN, draft("Piégée", "pas-un-flag", null)));
    }

    @Test
    void anAddressIsRequired() {
        assertThrows(InvalidInputException.class, () -> admin.createBox(ADMIN,
                new BoxDraft("Sans adresse", OperatingSystem.LINUX, Difficulty.EASY, "x", "  ", null, false, null,
                        null)));
    }

    @Test
    void twoMachinesCannotShareTheSameUrl() {
        admin.createBox(ADMIN, draft("Sentinel Bis", null, null));

        assertThrows(ConflictException.class, () -> admin.createBox(ADMIN, draft("Sentinel Bis", null, null)));
    }

    @Test
    void aPlayerCannotPublishModifyOrDelete() {
        Box box = admin.createBox(ADMIN, draft("Sentinel Bis", null, null)).box();

        assertThrows(ForbiddenException.class, () -> admin.createBox(PLAYER, draft("Autre", null, null)));
        assertThrows(ForbiddenException.class, () -> admin.updateBox(PLAYER, box.getSlug(), draft("X", null, null)));
        assertThrows(ForbiddenException.class, () -> admin.deleteBox(PLAYER, box.getSlug()));
        assertThrows(ForbiddenException.class, () -> admin.listBoxes(PLAYER));
    }

    @Test
    void deletingRemovesItFromTheCatalogue() {
        admin.createBox(ADMIN, draft("Sentinel Bis", null, null));

        admin.deleteBox(ADMIN, "sentinel-bis");

        assertEquals(0, admin.listBoxes(ADMIN).size());
        assertThrows(NotFoundException.class, () -> admin.deleteBox(ADMIN, "sentinel-bis"));
    }
}
