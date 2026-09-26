package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.fakes.InMemoryInstances;
import com.labplatform.application.fakes.InMemoryOwns;
import com.labplatform.application.fakes.InMemoryRatings;
import com.labplatform.application.fakes.InMemoryUsers;
import com.labplatform.application.fakes.InMemoryWriteups;
import com.labplatform.application.port.in.writeup.WriteupView;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.Role;
import com.labplatform.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteupServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");
    private static final String USER_FLAG = "11111111111111111111111111111111";
    private static final String ROOT_FLAG = "22222222222222222222222222222222";

    private Actor alice;
    private Actor bob;
    private Actor carol;
    private BoxService catalogue;
    private WriteupService writeups;

    @BeforeEach
    void setUp() {
        InMemoryBoxes boxes = new InMemoryBoxes();
        InMemoryOwns owns = new InMemoryOwns();
        InMemoryUsers users = new InMemoryUsers();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        alice = actor(users, "alice.durand@lab.test");
        bob = actor(users, "bob@lab.test");
        carol = actor(users, "carol@lab.test");
        boxes.save(Box.create("sentinel", "Sentinel", OperatingSystem.LINUX, Difficulty.EASY, "Synopsis.",
                "10.10.10.11", "cyberMans", NOW, Flag.ofSecret(USER_FLAG), Flag.ofSecret(ROOT_FLAG)));

        ScoreboardService scoreboard = new ScoreboardService(boxes, owns);
        catalogue = new BoxService(boxes, owns, new InMemoryRatings(), new InMemoryInstances(), scoreboard,
                Fakes.NO_TRANSACTION, clock);
        writeups = new WriteupService(boxes, owns, new InMemoryWriteups(), users, Fakes.NO_TRANSACTION, clock);
    }

    private static Actor actor(InMemoryUsers users, String email) {
        User saved = users.save(User.register(Email.of(email), "empreinte", NOW));
        return new Actor(saved.getId(), Role.USER);
    }

    private void pwn(Actor actor) {
        catalogue.submitFlag(actor, "sentinel", FlagKind.USER, USER_FLAG);
        catalogue.submitFlag(actor, "sentinel", FlagKind.ROOT, ROOT_FLAG);
    }

    @Test
    void writingRequiresHavingPwnedTheMachine() {
        assertThrows(ConflictException.class,
                () -> writeups.save(alice, "sentinel", "Ma méthode", "Contenu.", true));

        catalogue.submitFlag(alice, "sentinel", FlagKind.USER, USER_FLAG);
        // Un seul flag ne suffit pas : on n'a pas vu la moitié du travail.
        assertThrows(ConflictException.class,
                () -> writeups.save(alice, "sentinel", "Ma méthode", "Contenu.", true));

        catalogue.submitFlag(alice, "sentinel", FlagKind.ROOT, ROOT_FLAG);
        assertEquals("Ma méthode", writeups.save(alice, "sentinel", "Ma méthode", "Contenu.", true)
                .writeup().getTitle());
    }

    @Test
    void publishingSharesOnlyWithThoseWhoAlsoPwnedIt() {
        pwn(alice);
        writeups.save(alice, "sentinel", "Ma méthode", "Injection puis tâche planifiée.", true);

        // Bob cherche encore : il ne voit rien, publié ou non.
        assertEquals(0, writeups.listWriteups(bob, "sentinel").size());

        pwn(bob);
        List<WriteupView> visible = writeups.listWriteups(bob, "sentinel");
        assertEquals(1, visible.size());
        assertEquals("alice.durand", visible.get(0).handle());
        assertFalse(visible.get(0).mine());
    }

    @Test
    void anUnpublishedWriteupStaysPrivateToItsAuthor() {
        pwn(alice);
        pwn(bob);
        writeups.save(alice, "sentinel", "Brouillon", "À finir.", false);

        assertEquals(0, writeups.listWriteups(bob, "sentinel").size());
        assertEquals(1, writeups.listWriteups(alice, "sentinel").size());
        assertTrue(writeups.listWriteups(alice, "sentinel").get(0).mine());
    }

    @Test
    void savingTwiceRevisesTheSameWriteup() {
        pwn(alice);
        writeups.save(alice, "sentinel", "Première version", "Contenu.", false);

        WriteupView revised = writeups.save(alice, "sentinel", "Version revue", "Contenu enrichi.", true);

        assertEquals("Version revue", revised.writeup().getTitle());
        assertTrue(revised.writeup().isPublished());
        assertEquals(1, writeups.listWriteups(alice, "sentinel").size());
    }

    @Test
    void theAuthorSeesTheirOwnFirst() {
        pwn(alice);
        pwn(bob);
        writeups.save(alice, "sentinel", "Par Alice", "Contenu.", true);
        writeups.save(bob, "sentinel", "Par Bob", "Contenu.", true);

        assertTrue(writeups.listWriteups(bob, "sentinel").get(0).mine());
        assertTrue(writeups.listWriteups(alice, "sentinel").get(0).mine());
    }

    @Test
    void deletingRemovesOnlyTheCallersWriteup() {
        pwn(alice);
        pwn(bob);
        writeups.save(alice, "sentinel", "Par Alice", "Contenu.", true);
        writeups.save(bob, "sentinel", "Par Bob", "Contenu.", true);

        writeups.delete(alice, "sentinel");

        assertEquals(1, writeups.listWriteups(bob, "sentinel").size());
        assertEquals(1, writeups.listWriteups(alice, "sentinel").size());
        // Supprimer à nouveau ne casse rien.
        writeups.delete(alice, "sentinel");
    }

    @Test
    void anEmptyWriteupOrAnUnknownMachineIsRefused() {
        pwn(alice);

        assertThrows(InvalidInputException.class, () -> writeups.save(alice, "sentinel", "  ", "Contenu.", true));
        assertThrows(InvalidInputException.class, () -> writeups.save(alice, "sentinel", "Titre", "   ", true));
        assertThrows(NotFoundException.class, () -> writeups.listWriteups(alice, "inexistante"));
    }

    @Test
    void aWriteupNeverExposesTheAuthorsEmail() {
        pwn(alice);
        writeups.save(alice, "sentinel", "Ma méthode", "Contenu.", true);

        assertFalse(writeups.listWriteups(alice, "sentinel").get(0).handle().contains("@"));
    }
}
