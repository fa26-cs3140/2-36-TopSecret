import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

// Integration tests: menu + real SQLite missions + real SQLite audit log + real user file
class MissionMenuSqliteAuditLogTest {

    @TempDir
    Path dir;

    @Test
    void readsAndNewUsersAreRecordedWithUserAndTime() {
        Path db = dir.resolve("test.db");
        MissionStore store = new SqliteMissionStore(db);
        store.importFromTsv(SqliteMissionStore.DEFAULT_TSV_FILE);
        AuditLog audit = new SqliteAuditLog(db);
        AccountManager accounts = new AccountManager(
                new CipheredUserStore(dir.resolve("users.cip"), Path.of("ciphers", "key.txt")));
        UserAccount agent = accounts.createFirstAccount("agent", "Secret1");

        new MissionMenu(store, store, new Scanner("2\n1\n5\nbob\n4\n"),
                new PrintStream(new ByteArrayOutputStream()), audit, accounts, agent).run();

        List<AuditEntry> log = audit.getEntries();
        assertEquals(2, log.size());
        assertEquals("agent", log.get(0).getUsername());
        assertEquals(AuditLog.READ, log.get(0).getAction());
        assertEquals("Mission brief #1: Operation Sandtrap", log.get(0).getItem());
        assertFalse(log.get(0).getTimestamp().isBlank());
        assertEquals(AuditLog.CREATE, log.get(1).getAction());
        assertEquals("User account: bob", log.get(1).getItem());
        assertNotNull(accounts.findUser("bob"));
    }
}
