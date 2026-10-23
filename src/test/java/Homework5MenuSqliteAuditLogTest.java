import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

// Integration test: Homework5Menu + ModelDatabase (sample data) + SqliteAuditLog + real user file,
// all in one database file like the real program
class Homework5MenuSqliteAuditLogTest {

    @TempDir
    Path dir;

    @Test
    void adminAndStandardUserSessionsAreFullyAudited() throws Exception {
        Path dbFile = dir.resolve("test.db");
        AuditLog audit = new SqliteAuditLog(dbFile);
        AccountManager accounts = new AccountManager(
                new CipheredUserStore(dir.resolve("users.cip"), Path.of("ciphers", "key.txt")));
        UserAccount admin = accounts.createFirstAccount("agent", "Secret1");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
            ModelDatabase db = new ModelDatabase(connection);
            db.createTables();
            db.importSampleData(Path.of("database"));
            assertEquals(6, db.getFacilities().size());

            // admin: read brief 1, add a facility, add user bob, promote bob, view log
            String out = run(db, audit, accounts, admin,
                    "1\n2\n1\n0\n3\n3\nNew Site\nNS\n0\n5\nbob\n6\nbob\n1\n4\n0\n");
            assertTrue(out.contains("Title: Operation Sandtrap"));
            assertTrue(out.contains("7. New Site (NS)"));
            assertTrue(accounts.findUser("bob").isAdmin());

            // facility 1 has agents, so deleting it is refused
            assertTrue(run(db, audit, accounts, admin, "3\n5\n1\n0\n0\n").contains("Cannot delete this facility"));
            assertEquals(7, db.getFacilities().size());
        }

        List<AuditEntry> log = audit.getEntries();
        assertTrue(log.stream().anyMatch(e -> e.getAction().equals("READ") && e.getItem().equals("Brief #1")));
        assertTrue(log.stream().anyMatch(e -> e.getAction().equals("CREATE") && e.getItem().equals("Facility #7")));
        assertTrue(log.stream().anyMatch(e -> e.getItem().equals("User account: bob")));
        assertTrue(log.stream().anyMatch(e -> e.getItem().equals("User access: bob -> admin")));
        assertTrue(log.stream().allMatch(e -> e.getUsername().equals("agent") && !e.getTimestamp().isBlank()));
    }

    private String run(ModelDatabase db, AuditLog audit, AccountManager accounts, UserAccount user, String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new Homework5Menu(db, new Scanner(input), new PrintStream(output), audit, accounts, user, user.isAdmin()).run();
        return output.toString();
    }
}
