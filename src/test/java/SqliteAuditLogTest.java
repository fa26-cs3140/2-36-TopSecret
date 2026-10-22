import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SqliteAuditLogTest {

    @TempDir
    Path dir;

    private final Clock fixed = Clock.fixed(Instant.parse("2026-10-22T13:45:30.250Z"), ZoneOffset.UTC);

    @Test
    void newLogIsEmpty() {
        assertTrue(new SqliteAuditLog(dir.resolve("test.db"), fixed).getEntries().isEmpty());
    }

    @Test
    void recordSavesWhoWhatItemAndWhen() {
        AuditLog log = new SqliteAuditLog(dir.resolve("test.db"), fixed);
        log.record("smayan", AuditLog.READ, "Brief #3: Project Bluebird");
        AuditEntry e = log.getEntries().get(0);
        assertEquals(1, e.getId());
        assertEquals("2026-10-22T13:45:30", e.getTimestamp());
        assertEquals("smayan", e.getUsername());
        assertEquals("READ", e.getAction());
        assertEquals("Brief #3: Project Bluebird", e.getItem());
    }

    @Test
    void entriesComeBackOldestFirstAndSurviveRestart() {
        Path db = dir.resolve("test.db");
        new SqliteAuditLog(db, fixed).record("agent", AuditLog.CREATE, "Agent: Ghost");
        new SqliteAuditLog(db, fixed).record("bob", AuditLog.READ, "Agent: Ghost");
        List<AuditEntry> entries = new SqliteAuditLog(db).getEntries();
        assertEquals(2, entries.size());
        assertEquals("CREATE", entries.get(0).getAction());
        assertEquals("bob", entries.get(1).getUsername());
    }

    @Test
    void defaultClockRecordsCurrentTime() {
        AuditLog log = new SqliteAuditLog(dir.resolve("test.db"));
        log.record("agent", AuditLog.READ, "Facility: Rose Garden");
        assertFalse(log.getEntries().get(0).getTimestamp().isBlank());
    }

    @Test
    void blankFieldsAreRejected() {
        AuditLog log = new SqliteAuditLog(dir.resolve("test.db"), fixed);
        assertThrows(IllegalArgumentException.class, () -> log.record(" ", "READ", "x"));
        assertThrows(IllegalArgumentException.class, () -> log.record("agent", null, "x"));
        assertThrows(IllegalArgumentException.class, () -> log.record("agent", "READ", ""));
        assertTrue(log.getEntries().isEmpty());
    }

    @Test
    void createsMissingFolder() {
        new SqliteAuditLog(dir.resolve("new/folder/test.db"), fixed);
        assertTrue(Files.exists(dir.resolve("new/folder/test.db")));
    }

    @Test
    void badDatabasePathThrowsIllegalStateException() throws Exception {
        Files.createDirectory(dir.resolve("taken.db"));
        assertThrows(IllegalStateException.class, () -> new SqliteAuditLog(dir.resolve("taken.db"), fixed));
    }
}
