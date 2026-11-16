package view;

import model.AuditEntry;
import model.AuditLog;
import model.SessionStore;
import model.WebSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

// Tests for the admin web session menu (Member D).
// Disabled until SessionMenu is implemented.
@Disabled("SessionMenu is not implemented yet")
class SessionMenuTest {

    private final FakeSessions sessions = new FakeSessions();
    private final FakeAudit audit = new FakeAudit();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private SessionMenu menu(String input) {
        return new SessionMenu(sessions, audit, "admin",
                new Scanner(input), new PrintStream(output, true));
    }

    private String output() {
        return output.toString();
    }

    @Test
    void listShowsUserTokenAndLastActivity() {
        sessions.add("smayan", "abc123", LocalDateTime.of(2026, 11, 16, 12, 5, 30));

        menu("").listSessions();

        assertTrue(output().contains("smayan"), output());
        assertTrue(output().contains("abc123"), output());
        assertTrue(output().contains("2026-11-16 12:05:30"), output());
    }

    @Test
    void listNumbersEachSession() {
        sessions.add("smayan", "abc123", LocalDateTime.of(2026, 11, 16, 12, 0));
        sessions.add("ghost", "def456", LocalDateTime.of(2026, 11, 16, 12, 1));

        menu("").listSessions();

        assertTrue(output().contains("1. smayan"), output());
        assertTrue(output().contains("2. ghost"), output());
    }

    @Test
    void listWithNoSessionsSaysSo() {
        menu("").listSessions();

        assertTrue(output().contains("No active web sessions."), output());
    }

    @Test
    void deleteBySessionNumber() {
        sessions.add("smayan", "abc123", LocalDateTime.of(2026, 11, 16, 12, 0));
        sessions.add("ghost", "def456", LocalDateTime.of(2026, 11, 16, 12, 1));

        menu("2\n").deleteSession();

        assertEquals(List.of("def456"), sessions.deleted);
        assertTrue(output().contains("Session ended for ghost."), output());
    }

    @Test
    void deleteByFullToken() {
        sessions.add("smayan", "abc123", LocalDateTime.of(2026, 11, 16, 12, 0));

        menu("abc123\n").deleteSession();

        assertEquals(List.of("abc123"), sessions.deleted);
    }

    @Test
    void deleteIsAudited() {
        sessions.add("ghost", "def456", LocalDateTime.of(2026, 11, 16, 12, 0));

        menu("1\n").deleteSession();

        assertEquals(1, audit.entries.size());
        AuditEntry entry = audit.entries.get(0);
        assertEquals("admin", entry.getUsername());
        assertEquals(AuditLog.DELETE, entry.getAction());
        assertEquals("Web session for ghost", entry.getItem());
    }

    @Test
    void invalidChoiceDeletesNothing() {
        sessions.add("smayan", "abc123", LocalDateTime.of(2026, 11, 16, 12, 0));

        menu("9\n").deleteSession();

        assertTrue(sessions.deleted.isEmpty());
        assertTrue(audit.entries.isEmpty());
        assertTrue(output().contains("No session matches that choice."), output());
    }

    @Test
    void deleteWithNoSessionsDoesNotAsk() {
        menu("1\n").deleteSession();

        assertTrue(sessions.deleted.isEmpty());
        assertTrue(output().contains("No active web sessions."), output());
    }

    @Test
    void runListsThenDeletesThenGoesBack() {
        sessions.add("ghost", "def456", LocalDateTime.of(2026, 11, 16, 12, 0));

        menu("1\n2\n1\n0\n").run();

        assertTrue(output().contains("def456"), output());
        assertEquals(List.of("def456"), sessions.deleted);
    }

    @Test
    void runRejectsUnknownOption() {
        menu("7\n0\n").run();

        assertTrue(output().contains("Invalid choice."), output());
    }

    @Test
    void runStopsAtEndOfInput() {
        menu("").run();

        assertTrue(output().contains("WEB SESSIONS"), output());
    }

    // In-memory session store so the menu can be tested without a database
    private static class FakeSessions implements SessionStore {
        final List<WebSession> active = new ArrayList<>();
        final List<String> deleted = new ArrayList<>();

        void add(String user, String token, LocalDateTime lastActivity) {
            active.add(new WebSession(user, token, lastActivity));
        }

        @Override
        public String create(String username) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String touch(String token) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<WebSession> list() {
            return new ArrayList<>(active);
        }

        @Override
        public boolean delete(String token) {
            deleted.add(token);
            return active.removeIf(s -> s.getToken().equals(token));
        }
    }

    // Collects audit events in memory
    private static class FakeAudit implements AuditLog {
        final List<AuditEntry> entries = new ArrayList<>();

        @Override
        public void record(String username, String action, String item) {
            entries.add(new AuditEntry(entries.size() + 1, "now", username, action, item));
        }

        @Override
        public List<AuditEntry> getEntries() {
            return entries;
        }
    }
}
