import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SqliteMessageStoreTest {

    @TempDir
    Path dir;

    private Path db;
    private final Clock fixed = Clock.fixed(Instant.parse("2026-10-29T18:10:00Z"), ZoneOffset.UTC);

    // A database with two agents, like the real one after the sample data loads
    @BeforeEach
    void setUp() throws Exception {
        db = dir.resolve("test.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            ModelDatabase model = new ModelDatabase(c);
            model.createTables();
            int f = model.addFacility("Rose Garden", "RG");
            model.addAgent("Ghost", null, null, null, f);
            model.addAgent("Siren", null, null, null, f);
        }
    }

    @Test
    void sendSavesMessageForAgentIgnoringCase() {
        MessageStore store = new SqliteMessageStore(db, fixed);
        Message m = store.send("smayan", "gHoSt", "  Meet at the safehouse.  ");
        assertEquals(1, m.getId());
        assertEquals("Ghost", m.getAgentName());
        assertEquals("smayan", m.getSender());
        assertEquals("Meet at the safehouse.", m.getText());
        assertEquals("2026-10-29T18:10", m.getSentAt().substring(0, 16));
    }

    @Test
    void onlyTheMatchingAgentSeesTheirMessages() {
        MessageStore store = new SqliteMessageStore(db, fixed);
        store.send("smayan", "Ghost", "first");
        store.send("lina", "ghost", "second");
        store.send("smayan", "Siren", "for siren");

        List<Message> ghost = store.getMessagesFor("GHOST");
        assertEquals(2, ghost.size());
        assertEquals("first", ghost.get(0).getText());
        assertEquals("lina", ghost.get(1).getSender());
        assertEquals(1, store.getMessagesFor("siren").size());
        assertTrue(store.getMessagesFor("smayan").isEmpty());
        assertTrue(store.getMessagesFor(" ").isEmpty());
        assertTrue(store.getMessagesFor(null).isEmpty());
    }

    @Test
    void messagesSurviveRestart() {
        new SqliteMessageStore(db, fixed).send("smayan", "Ghost", "hello");
        assertEquals(1, new SqliteMessageStore(db).getMessagesFor("ghost").size());
    }

    @Test
    void cannotMessageAnAgentThatDoesNotExist() {
        MessageStore store = new SqliteMessageStore(db, fixed);
        assertThrows(IllegalArgumentException.class, () -> store.send("smayan", "Nobody", "hi"));
        assertThrows(IllegalArgumentException.class, () -> store.send("smayan", null, "hi"));
    }

    @Test
    void blankTextOrSenderIsRejected() {
        MessageStore store = new SqliteMessageStore(db, fixed);
        assertThrows(IllegalArgumentException.class, () -> store.send("smayan", "Ghost", "  "));
        assertThrows(IllegalArgumentException.class, () -> store.send("smayan", "Ghost", null));
        assertThrows(IllegalArgumentException.class, () -> store.send(" ", "Ghost", "hi"));
        assertThrows(IllegalArgumentException.class, () -> store.send(null, "Ghost", "hi"));
        assertTrue(store.getMessagesFor("ghost").isEmpty());
    }

    @Test
    void deletingAnAgentDeletesTheirMessages() throws Exception {
        MessageStore store = new SqliteMessageStore(db, fixed);
        store.send("smayan", "Ghost", "hello");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            assertTrue(new ModelDatabase(c).deleteAgent(1));
            try (Statement s = c.createStatement()) {
                var rs = s.executeQuery("SELECT COUNT(*) FROM messages");
                rs.next();
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    @Test
    void missingAgentsTableGivesClearErrors() {
        MessageStore store = new SqliteMessageStore(dir.resolve("empty.db"), fixed);
        assertThrows(IllegalStateException.class, () -> store.send("smayan", "Ghost", "hi"));
        assertThrows(IllegalStateException.class, () -> store.getMessagesFor("ghost"));
    }

    @Test
    void badDatabasePathThrows() throws Exception {
        Files.createDirectory(dir.resolve("taken.db"));
        assertThrows(IllegalStateException.class, () -> new SqliteMessageStore(dir.resolve("taken.db")));
    }
}
