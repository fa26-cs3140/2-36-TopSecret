package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests for web session timeout and the admin session list (Member D).
class SqliteSessionStoreTest {

    @TempDir
    Path dir;

    private TestClock clock;
    private SqliteSessionStore store;

    @BeforeEach
    void setUp() {
        clock = new TestClock(Instant.parse("2026-11-16T12:00:00Z"));
        store = new SqliteSessionStore(dir.resolve("test.db"), clock);
    }

    @Test
    void createReturnsTokenThatBelongsToUser() {
        String token = store.create("smayan");

        assertNotNull(token);
        assertEquals("smayan", store.touch(token));
    }

    @Test
    void eachLoginGetsDifferentToken() {
        String first = store.create("smayan");
        String second = store.create("smayan");

        assertNotEquals(first, second);
        assertEquals(2, store.list().size());
    }

    @Test
    void createRejectsBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> store.create(" "));
        assertThrows(IllegalArgumentException.class, () -> store.create(null));
    }

    @Test
    void unknownOrMissingTokenIsNotValid() {
        assertNull(store.touch("not-a-real-token"));
        assertNull(store.touch(null));
    }

    @Test
    void sessionIsValidJustBeforeThirtyMinutes() {
        String token = store.create("smayan");

        clock.advance(Duration.ofMinutes(30).minusSeconds(1));

        assertEquals("smayan", store.touch(token));
    }

    @Test
    void sessionTimesOutAfterThirtyMinutesIdle() {
        String token = store.create("smayan");

        clock.advance(Duration.ofMinutes(30));

        assertNull(store.touch(token));
    }

    @Test
    void eachRequestResetsTheTimer() {
        String token = store.create("smayan");

        clock.advance(Duration.ofMinutes(20));
        assertEquals("smayan", store.touch(token));
        clock.advance(Duration.ofMinutes(20));

        assertEquals("smayan", store.touch(token), "40 minutes total, but only 20 idle");
    }

    @Test
    void listShowsUserTokenAndLastActivity() {
        String token = store.create("smayan");
        clock.advance(Duration.ofMinutes(5));
        store.touch(token);

        List<WebSession> sessions = store.list();

        assertEquals(1, sessions.size());
        assertEquals("smayan", sessions.get(0).getUsername());
        assertEquals(token, sessions.get(0).getToken());
        assertEquals(LocalDateTime.of(2026, 11, 16, 12, 5), sessions.get(0).getLastActivity());
    }

    @Test
    void listIsOrderedOldestActivityFirst() {
        String ghost = store.create("ghost");
        clock.advance(Duration.ofMinutes(1));
        store.create("smayan");
        clock.advance(Duration.ofMinutes(1));
        store.touch(ghost);

        List<WebSession> sessions = store.list();

        assertEquals("smayan", sessions.get(0).getUsername());
        assertEquals("ghost", sessions.get(1).getUsername());
    }

    @Test
    void timedOutSessionsAreNotListed() {
        store.create("ghost");
        clock.advance(Duration.ofMinutes(25));
        store.create("smayan");
        clock.advance(Duration.ofMinutes(10));

        List<WebSession> sessions = store.list();

        assertEquals(1, sessions.size());
        assertEquals("smayan", sessions.get(0).getUsername());
    }

    @Test
    void deletedTokenMustLogInAgain() {
        String token = store.create("smayan");

        assertTrue(store.delete(token));
        assertNull(store.touch(token));
        assertTrue(store.list().isEmpty());
    }

    @Test
    void deletingUnknownTokenReturnsFalse() {
        assertFalse(store.delete("not-a-real-token"));
        assertFalse(store.delete(null));
    }

    @Test
    void deletingOneSessionKeepsTheOthers() {
        String first = store.create("smayan");
        String second = store.create("ghost");

        store.delete(first);

        assertNull(store.touch(first));
        assertEquals("ghost", store.touch(second));
    }

    @Test
    void anotherStoreOnSameDatabaseSeesTheSessions() {
        // The terminal and the web server are separate programs sharing one database
        String token = store.create("smayan");
        SqliteSessionStore terminal = new SqliteSessionStore(dir.resolve("test.db"), clock);

        assertEquals(1, terminal.list().size());
        assertTrue(terminal.delete(token));
        assertNull(store.touch(token));
    }

    // A clock the tests can move forward to check the timeout
    private static class TestClock extends Clock {
        private Instant now;

        TestClock(Instant start) {
            now = start;
        }

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
