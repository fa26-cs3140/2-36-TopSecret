import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Unit tests for the Homework 5 menu. The audit log and the user file are Mockito mocks;
// the model uses an in-memory database so nothing is written to disk.
class Homework5MenuTest {

    private final AuditLog audit = mock(AuditLog.class);
    private final UserStore users = mock(UserStore.class);
    private final UserAccount admin = new UserAccount("agent", "Secret1", true);
    private final UserAccount standard = new UserAccount("bob", "BobPass", false);
    private Connection connection;
    private ModelDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new ModelDatabase(connection);
        db.createTables();
        int hq = db.addFacility("Rose Garden", "RG");
        db.addAgent("Ghost", "1992-11-22", null, "field agent", hq);
        db.addBrief("Operation Sandtrap", "1970-11-03", "Bug the pouch.");
        when(users.loadAll()).thenReturn(new ArrayList<>(List.of(admin, standard)));
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    // Runs the menu as this user with the typed input and returns what it printed
    private String run(UserAccount user, String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new Homework5Menu(db, new Scanner(input), new PrintStream(output), audit,
                new AccountManager(users), user, user.isAdmin()).run();
        return output.toString();
    }

    // ---------- menus and access ----------

    @Test
    void adminSeesAdminOptions() {
        String out = run(admin, "1\n0\n0\n");
        assertTrue(out.contains("4. View Audit Log"));
        assertTrue(out.contains("6. Manage User Access"));
        assertTrue(out.contains("3. Add an entry"));
        assertTrue(out.contains("5. Delete an entry"));
    }

    @Test
    void standardUserDoesNotSeeOrUseAdminOptions() {
        String out = run(standard, "4\n5\n6\n1\n3\n4\n5\n0\n0\n");
        assertFalse(out.contains("View Audit Log"));
        assertFalse(out.contains("Add User"));
        assertFalse(out.contains("Add an entry"));
        assertFalse(out.contains("Delete an entry"));
        assertTrue(out.contains("Invalid choice."));
        verify(audit, never()).getEntries();
        verify(users, never()).saveAll(anyList());
    }

    @Test
    void invalidChoicesAndEndOfInputAreHandled() {
        assertTrue(run(admin, "9\n1\n9\n").contains("Invalid choice."));
        assertNotNull(run(admin, ""));
    }

    // ---------- list and read (each read is audited) ----------

    @Test
    void listsAllThreeTypesAndAuditsReads() {
        String out = run(standard, "1\n1\n0\n2\n1\n0\n3\n1\n0\n0\n");
        assertTrue(out.contains("1. Operation Sandtrap"));
        assertTrue(out.contains("1. Ghost"));
        assertTrue(out.contains("1. Rose Garden (RG)"));
        verify(audit).record("bob", AuditLog.READ, "Brief #1");
        verify(audit).record("bob", AuditLog.READ, "Agent #1");
        verify(audit).record("bob", AuditLog.READ, "Facility #1");
    }

    @Test
    void emptyListsSayNoEntries() throws Exception {
        db.deleteBrief(1);
        db.deleteAgent(1);
        db.deleteFacility(1);
        String out = run(admin, "1\n1\n0\n2\n1\n0\n3\n1\n0\n0\n");
        assertTrue(out.contains("No briefs available."));
        assertTrue(out.contains("No agents available."));
        assertTrue(out.contains("No facilities available."));
    }

    @Test
    void readsOneEntryOfEachType() {
        String out = run(standard, "1\n2\n1\n0\n2\n2\n1\n0\n3\n2\n1\n0\n0\n");
        assertTrue(out.contains("Text: Bug the pouch."));
        assertTrue(out.contains("Notes: field agent"));
        assertTrue(out.contains("Abbreviation: RG"));
        verify(audit, times(1)).record("bob", AuditLog.READ, "Brief #1");
    }

    @Test
    void readHandlesBadAndMissingIds() {
        String out = run(admin, "1\n2\nabc\n2\n99\n2\n0\n");
        assertTrue(out.contains("Please enter a valid number."));
        assertTrue(out.contains("Entry not found."));
    }

    // ---------- add (immediately visible, audited as CREATE) ----------

    @Test
    void addsBriefFacilityAndAgent() throws Exception {
        String out = run(admin,
                "1\n3\nNew Op\n2026-10-23\nSome text\n0\n"
              + "3\n3\nGlass House\nGA\n0\n"
              + "2\n3\nWraith\n1993-12-08\n\nnotes\n2\n0\n0\n");
        assertTrue(out.contains("2. New Op"));
        assertTrue(out.contains("2. Glass House (GA)"));
        assertTrue(out.contains("2. Wraith"));
        assertEquals(2, db.getBriefs().size());
        assertEquals(2, db.getAgents().get(1).getFacilityId());
        verify(audit).record("agent", AuditLog.CREATE, "Brief #2");
        verify(audit).record("agent", AuditLog.CREATE, "Facility #2");
        verify(audit).record("agent", AuditLog.CREATE, "Agent #2");
    }

    @Test
    void addRejectsBlankFieldsAndBadFacility() throws Exception {
        String out = run(admin,
                "1\n3\n\n2026\ntext\n0\n"
              + "3\n3\n\nGA\n0\n"
              + "2\n3\n\nx\nx\nx\n"
              + "3\nGhost2\nx\n\nx\nabc\n"
              + "3\nGhost3\nx\n\nx\n99\n0\n0\n");
        assertTrue(out.contains("All fields are required."));
        assertTrue(out.contains("Agent name is required."));
        assertTrue(out.contains("Invalid facility ID."));
        assertTrue(out.contains("Facility does not exist."));
        assertEquals(1, db.getAgents().size());
        verify(audit, never()).record(anyString(), eq(AuditLog.CREATE), anyString());
    }

    @Test
    void addAgentNeedsAFacilityFirst() throws Exception {
        db.deleteAgent(1);
        db.deleteFacility(1);
        assertTrue(run(admin, "2\n3\nGhost\nx\n\nx\n0\n0\n").contains("Add a facility before adding an agent."));
    }

    @Test
    void addStopsWhenInputEnds() throws Exception {
        run(admin, "1\n3\nOnly title\n");
        run(admin, "3\n3\nOnly name\n");
        run(admin, "2\n3\nOnly name\n");
        run(admin, "2\n3\nName\nx\n\nx\n");
        assertEquals(1, db.getBriefs().size());
        assertEquals(1, db.getFacilities().size());
        assertEquals(1, db.getAgents().size());
    }

    // ---------- update (audited as UPDATE) ----------

    @Test
    void updatesEachType() throws Exception {
        run(admin, "1\n4\n1\nNew Title\n1971-01-01\nNew text\n0\n"
                 + "3\n4\n1\nRose Annex\nRA\n0\n"
                 + "2\n4\n1\nGhost Prime\n1992-11-22\n\nupdated\n1\n0\n0\n");
        assertEquals("New Title", db.getBriefs().get(0).getTitle());
        assertEquals("RA", db.getFacilities().get(0).getAbbreviation());
        assertEquals("updated", db.getAgents().get(0).getNotes());
        verify(audit).record("agent", AuditLog.UPDATE, "Brief #1");
        verify(audit).record("agent", AuditLog.UPDATE, "Facility #1");
        verify(audit).record("agent", AuditLog.UPDATE, "Agent #1");
    }

    @Test
    void updateRejectsMissingEntriesAndBadInput() throws Exception {
        String out = run(admin,
                "1\n4\nabc\n4\n99\n4\n1\n\nx\nx\n0\n"
              + "3\n4\n99\n4\n1\n\nRA\n0\n"
              + "2\n4\n99\n4\n1\n\nx\nx\nx\n"
              + "4\n1\nName\nx\n\nx\nabc\n"
              + "4\n1\nName\nx\n\nx\n99\n0\n0\n");
        assertTrue(out.contains("Invalid ID."));
        assertTrue(out.contains("Brief not found."));
        assertTrue(out.contains("Facility not found."));
        assertTrue(out.contains("Agent not found."));
        assertTrue(out.contains("All fields are required."));
        assertTrue(out.contains("Name is required."));
        assertTrue(out.contains("Invalid facility ID."));
        assertTrue(out.contains("Facility does not exist."));
        assertEquals("Operation Sandtrap", db.getBriefs().get(0).getTitle());
        verify(audit, never()).record(anyString(), eq(AuditLog.UPDATE), anyString());
    }

    @Test
    void updateStopsWhenInputEnds() throws Exception {
        run(admin, "1\n4\n");
        run(admin, "1\n4\n1\nTitle\n");
        run(admin, "3\n4\n1\nName\n");
        run(admin, "2\n4\n1\nName\n");
        run(admin, "2\n4\n1\nName\nx\n\nx\n");
        assertEquals("Operation Sandtrap", db.getBriefs().get(0).getTitle());
        assertEquals("Rose Garden", db.getFacilities().get(0).getName());
        assertEquals("Ghost", db.getAgents().get(0).getName());
    }

    // ---------- delete (integrity kept, audited as DELETE) ----------

    @Test
    void deleteFacilityWithAgentsIsRefused() throws Exception {
        String out = run(admin, "3\n5\n1\n0\n0\n");
        assertTrue(out.contains("Cannot delete this facility"));
        assertEquals(1, db.getFacilities().size());
        verify(audit, never()).record(anyString(), eq(AuditLog.DELETE), anyString());
    }

    @Test
    void deletesEachTypeInASafeOrder() throws Exception {
        String out = run(admin, "1\n5\n1\n0\n2\n5\n1\n0\n3\n5\n1\n0\n0\n");
        assertTrue(out.contains("Brief #1 deleted."));
        assertTrue(out.contains("Agent #1 deleted."));
        assertTrue(out.contains("Facility #1 deleted."));
        assertTrue(db.getFacilities().isEmpty());
        verify(audit).record("agent", AuditLog.DELETE, "Facility #1");
    }

    @Test
    void deleteHandlesBadAndMissingIds() {
        String out = run(admin, "1\n5\nabc\n5\n99\n5\n\n0\n0\n");
        assertTrue(out.contains("Invalid ID."));
        assertTrue(out.contains("Entry not found."));
        assertNotNull(run(admin, "1\n5\n"));
    }

    // ---------- audit log, add user, access levels ----------

    @Test
    void adminViewsAuditLog() {
        when(audit.getEntries()).thenReturn(List.of(new AuditEntry(1, "2026-10-23T10:00:00", "bob", "READ", "Brief #1")));
        assertTrue(run(admin, "4\n0\n").contains("2026-10-23T10:00:00  bob  READ  Brief #1"));
    }

    @Test
    void emptyAuditLogSaysSo() {
        assertTrue(run(admin, "4\n0\n").contains("No audit entries available."));
    }

    @Test
    void adminAddsUserAndItIsAudited() {
        String out = run(admin, "5\ncarol\n0\n");
        assertTrue(out.contains("User created successfully."));
        verify(users).saveAll(anyList());
        verify(audit).record("agent", AuditLog.CREATE, "User account: carol");
    }

    @Test
    void addUserShowsErrorsAndHandlesEndOfInput() {
        assertTrue(run(admin, "5\nbob\n0\n").contains("Error: Username already exists"));
        run(admin, "5\n");
        verify(users, never()).saveAll(anyList());
    }

    @Test
    void adminPromotesAndDemotesOthers() {
        String out = run(admin, "6\nbob\n1\n6\nbob\n2\n0\n");
        assertTrue(out.contains("bob - standard"));
        assertTrue(out.contains("bob is now an admin."));
        assertTrue(out.contains("bob is now a standard user."));
        verify(audit).record("agent", AuditLog.UPDATE, "User access: bob -> admin");
        verify(audit).record("agent", AuditLog.UPDATE, "User access: bob -> standard");
    }

    @Test
    void adminCannotChangeOwnLevelAndBadInputIsRejected() {
        String out = run(admin, "6\nagent\n2\n6\nbob\n7\n6\nnobody\n1\n0\n");
        assertTrue(out.contains("Error: You cannot change your own access level"));
        assertTrue(out.contains("Invalid choice."));
        assertTrue(out.contains("Error: No user named nobody"));
        run(admin, "6\n");
        run(admin, "6\nbob\n");
        verify(users, never()).saveAll(anyList());
    }

    @Test
    void adminOnlyHelpersRefuseStandardUsersEvenIfFlagIsWrong() {
        // admin=true is passed but the user is not an admin, so the menu still treats them as standard
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new Homework5Menu(db, new Scanner("4\n0\n"), new PrintStream(output), audit,
                new AccountManager(users), standard, true).run();
        assertFalse(output.toString().contains("View Audit Log"));
    }
}
