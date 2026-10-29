import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Unit tests: the message store and audit log are Mockito mocks.
// Agents come from an in-memory database.
@Disabled("MessageMenu is a stub until week 2")
class MessageMenuTest {

    private final MessageStore store = mock(MessageStore.class);
    private final AuditLog audit = mock(AuditLog.class);
    private final UserAccount smayan = new UserAccount("smayan", "Secret1", false);
    private final UserAccount ghost = new UserAccount("ghost", "Secret1", false);
    private Connection connection;
    private ModelDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new ModelDatabase(connection);
        db.createTables();
        int f = db.addFacility("Rose Garden", "RG");
        db.addAgent("Ghost", null, null, null, f);
        db.addAgent("Siren", null, null, null, f);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    private String run(UserAccount user, String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new MessageMenu(store, db, audit, user, new Scanner(input), new PrintStream(output)).run();
        return output.toString();
    }

    @Test
    void sendByAgentNumberSavesAndAudits() {
        when(store.send("smayan", "Ghost", "Meet at noon.")).thenReturn(
                new Message(7, "Ghost", "smayan", "Meet at noon.", "2026-10-29T14:00:00"));
        String out = run(smayan, "1\n1\nMeet at noon.\n0\n");
        assertTrue(out.contains("1. Ghost"));
        assertTrue(out.contains("2. Siren"));
        assertTrue(out.contains("Message sent to Ghost."));
        verify(audit).record("smayan", AuditLog.CREATE, "Message #7 to Ghost");
    }

    @Test
    void sendByAgentNameIgnoringCase() {
        when(store.send("smayan", "Siren", "hi")).thenReturn(
                new Message(1, "Siren", "smayan", "hi", "2026-10-29T14:00:00"));
        assertTrue(run(smayan, "1\nsIrEn\nhi\n0\n").contains("Message sent to Siren."));
        verify(store).send("smayan", "Siren", "hi");
    }

    @Test
    void unknownAgentIsRejected() {
        String out = run(smayan, "1\nNobody\n0\n");
        assertTrue(out.contains("No agent matches 'Nobody'."));
        verify(store, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void storeErrorsAreShownAndNotAudited() {
        when(store.send("smayan", "Ghost", "")).thenThrow(new IllegalArgumentException("Message text cannot be empty"));
        String out = run(smayan, "1\nGhost\n\n0\n");
        assertTrue(out.contains("Error: Message text cannot be empty"));
        verify(audit, never()).record(anyString(), anyString(), anyString());
    }

    @Test
    void noAgentsMeansNothingToSend() throws Exception {
        db.deleteAgent(1);
        db.deleteAgent(2);
        assertTrue(run(smayan, "1\n0\n").contains("There are no agents to message."));
    }

    @Test
    void databaseErrorIsShown() throws Exception {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("DROP TABLE agents");
        }
        assertTrue(run(smayan, "1\n0\n").contains("Database error"));
    }

    @Test
    void readShowsOnlyMyMessagesAndAuditsEach() {
        when(store.getMessagesFor("ghost")).thenReturn(List.of(
                new Message(3, "Ghost", "smayan", "first", "2026-10-29T14:00:00"),
                new Message(5, "Ghost", "lina", "second", "2026-10-29T15:00:00")));
        String out = run(ghost, "2\n0\n");
        assertTrue(out.contains("2. Read my messages (2)"));
        assertTrue(out.contains("Messages for Ghost:"));
        assertTrue(out.contains("2026-10-29T14:00:00  from smayan: first"));
        assertTrue(out.contains("from lina: second"));
        verify(audit).record("ghost", AuditLog.READ, "Message #3");
        verify(audit).record("ghost", AuditLog.READ, "Message #5");
    }

    @Test
    void noMessagesSaysSo() {
        assertTrue(run(smayan, "2\n0\n").contains("You have no messages."));
        verify(audit, never()).record(anyString(), anyString(), anyString());
    }

    @Test
    void invalidChoiceAndEndOfInput() {
        assertTrue(run(smayan, "9\n").contains("Invalid choice."));
        run(smayan, "1\n");
        run(smayan, "1\nGhost\n");
        verify(store, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void inboxNoticeOnlyWhenThereAreMessages() {
        when(store.getMessagesFor("ghost")).thenReturn(List.of(
                new Message(3, "Ghost", "smayan", "first", "t")));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new MessageMenu(store, db, audit, ghost, new Scanner(""), new PrintStream(output)).printInboxNotice();
        assertEquals("You have 1 message. Open Messages to read them.", output.toString().trim());

        when(store.getMessagesFor("ghost")).thenReturn(List.of(
                new Message(3, "Ghost", "smayan", "a", "t"), new Message(4, "Ghost", "lina", "b", "t")));
        output.reset();
        new MessageMenu(store, db, audit, ghost, new Scanner(""), new PrintStream(output)).printInboxNotice();
        assertTrue(output.toString().contains("You have 2 messages."));

        output.reset();
        new MessageMenu(store, db, audit, smayan, new Scanner(""), new PrintStream(output)).printInboxNotice();
        assertEquals("", output.toString());
    }
}
