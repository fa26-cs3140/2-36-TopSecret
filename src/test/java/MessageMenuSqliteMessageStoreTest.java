import org.junit.jupiter.api.Disabled;
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

// Integration test of the full messages workflow from the assignment:
// real sample data, real user file, real login, real message store and audit log.
@Disabled("MessageMenu is a stub until week 2")
class MessageMenuSqliteMessageStoreTest {

    @TempDir
    Path dir;

    @Test
    void messageToGhostIsReadOnlyByUserNamedGhost() throws Exception {
        Path dbFile = dir.resolve("test.db");
        AccountManager accounts = new AccountManager(
                new CipheredUserStore(dir.resolve("users.cip"), Path.of("ciphers", "key.txt")));
        AuditLog audit = new SqliteAuditLog(dbFile);
        MessageStore messages = new SqliteMessageStore(dbFile);

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
            ModelDatabase db = new ModelDatabase(connection);
            db.createTables();
            db.importSampleData(Path.of("database"));   // agents include Ghost (#2)

            // 1. smayan logs in and sends a message to Ghost (picked by name, any case)
            UserAccount smayan = accounts.createFirstAccount("smayan", "Secret1");
            String sent = run(messages, db, audit, smayan, "1\nghost\nPackage is at the drop point.\n0\n");
            assertTrue(sent.contains("Message sent to Ghost."));

            // 2. an account named "Ghost" is created and logs in for the first time
            accounts.addUser(smayan, "Ghost");
            UserAccount ghostUser = new LoginHandler(accounts,
                    new Scanner("Ghost\nGhostPass1\nGhostPass1\n"),
                    new PrintStream(new ByteArrayOutputStream())).authenticate();
            assertNotNull(ghostUser);

            // 3. Ghost sees the message, smayan (not an agent) sees nothing
            String ghostView = run(messages, db, audit, ghostUser, "2\n0\n");
            assertTrue(ghostView.contains("from smayan: Package is at the drop point."));
            assertTrue(run(messages, db, audit, smayan, "2\n0\n").contains("You have no messages."));

            // 4. another agent's account cannot see Ghost's mail
            accounts.addUser(smayan, "siren");
            assertTrue(messages.getMessagesFor("siren").isEmpty());
        }

        List<AuditEntry> log = audit.getEntries();
        assertTrue(log.stream().anyMatch(e -> e.getUsername().equals("smayan")
                && e.getAction().equals("CREATE") && e.getItem().equals("Message #1 to Ghost")));
        assertTrue(log.stream().anyMatch(e -> e.getUsername().equals("Ghost")
                && e.getAction().equals("READ") && e.getItem().equals("Message #1")));
    }

    private String run(MessageStore messages, ModelDatabase db, AuditLog audit,
                       UserAccount user, String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new MessageMenu(messages, db, audit, user, new Scanner(input), new PrintStream(output)).run();
        return output.toString();
    }
}
