package com.labplatform.application.service;

import com.labplatform.application.fakes.InMemoryJournal;
import com.labplatform.application.fakes.InMemoryUsers;
import com.labplatform.application.port.in.journal.JournalLine;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.shared.ForbiddenException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.Role;
import com.labplatform.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");
    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Actor BOB = new Actor(2L, Role.USER);
    private static final Actor ADMIN = new Actor(9L, Role.ADMIN);

    private InMemoryJournal journal;
    private JournalService service;

    @BeforeEach
    void setUp() {
        journal = new InMemoryJournal();
        InMemoryUsers users = new InMemoryUsers();
        users.save(User.restore(1L, Email.of("alice@example.com"), "hash", Role.USER, NOW, null));
        users.save(User.restore(2L, Email.of("bob@example.com"), "hash", Role.USER, NOW, null));
        users.save(User.restore(9L, Email.of("admin@example.com"), "hash", Role.ADMIN, NOW, null));
        service = new JournalService(journal, users);

        journal.record(JournalEvent.of(1L, JournalKind.FLAG_VALIDATED, "sentinel", "USER", NOW));
        journal.record(JournalEvent.of(2L, JournalKind.COURSE_VIEWED, "traces-disque", NOW.plusSeconds(60)));
        journal.record(JournalEvent.of(1L, JournalKind.BOX_PWNED, "sentinel", NOW.plusSeconds(120)));
    }

    @Test
    void aPlayerOnlyReadsItsOwnJournal() {
        List<JournalLine> mine = service.myJournal(ALICE, 20);

        assertEquals(2, mine.size());
        assertTrue(mine.stream().allMatch(line -> line.event().getUserId().equals(1L)));
        // Le plus récent d'abord.
        assertEquals(JournalKind.BOX_PWNED, mine.get(0).event().getKind());
    }

    @Test
    void theAdministratorReadsTheWholePlatform() {
        List<JournalLine> all = service.platformJournal(ADMIN, 20);

        assertEquals(3, all.size());
        assertEquals(JournalKind.BOX_PWNED, all.get(0).event().getKind());
    }

    @Test
    void aPlayerCannotReadThePlatformJournal() {
        assertThrows(ForbiddenException.class, () -> service.platformJournal(BOB, 20));
    }

    /** Un journal d'exploitation se lit à plusieurs : il ne distribue pas d'adresses. */
    @Test
    void authorsAppearByHandleNeverByEmail() {
        List<JournalLine> all = service.platformJournal(ADMIN, 20);

        assertTrue(all.stream().map(JournalLine::handle).anyMatch("alice"::equals));
        assertTrue(all.stream().map(JournalLine::handle).noneMatch(handle -> handle.contains("@")));
    }

    /** Le journal survit à son auteur : une ligne orpheline reste lisible. */
    @Test
    void anEventOfADeletedAccountStaysReadable() {
        journal.record(JournalEvent.of(404L, JournalKind.REGISTERED, null, NOW.plusSeconds(180)));

        List<JournalLine> all = service.platformJournal(ADMIN, 20);

        assertEquals("compte supprimé", all.get(0).handle());
    }

    @Test
    void theLimitIsHonoured() {
        assertEquals(1, service.platformJournal(ADMIN, 1).size());
        assertEquals(1, service.myJournal(ALICE, 1).size());
    }
}
