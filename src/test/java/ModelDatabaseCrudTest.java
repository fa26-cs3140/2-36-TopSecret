import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// Update, delete (integrity), and sample-data import for ModelDatabase
class ModelDatabaseCrudTest {

    @TempDir
    Path dir;

    private Connection connection;
    private ModelDatabase db;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new ModelDatabase(connection);
        db.createTables();
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    private void createMissionsTable() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE missions (id INTEGER PRIMARY KEY, title TEXT, brief TEXT, date TEXT)");
            s.executeUpdate("INSERT INTO missions VALUES (1, 'Op', 'b', '2026-01-01')");
        }
    }

    @Test
    void connectionMustUseAutoCommit() throws Exception {
        connection.setAutoCommit(false);
        assertThrows(SQLException.class, () -> new ModelDatabase(connection));
        connection.setAutoCommit(true);
    }

    @Test
    void updatesChangeStoredValues() throws Exception {
        int f = db.addFacility("Rose Garden", "RG");
        int f2 = db.addFacility("Glass House", "GA");
        int a = db.addAgent("Ghost", null, null, null, f);
        int b = db.addBrief("Old", "2000-01-01", "old");

        assertTrue(db.updateBrief(b, "New", "2001-01-01", "new"));
        assertTrue(db.updateFacility(f, "Rose Annex", "RA"));
        assertTrue(db.updateAgent(a, "Wraith", "1993-12-08", null, "moved", f2));

        assertEquals("New", db.getBriefs().get(0).getTitle());
        assertEquals("RA", db.getFacilities().get(0).getAbbreviation());
        assertEquals(f2, db.getAgents().get(0).getFacilityId());
        assertFalse(db.updateBrief(99, "x", "x", "x"));
        assertFalse(db.updateFacility(99, "x", "x"));
        assertFalse(db.updateAgent(99, "x", null, null, null, f));
    }

    @Test
    void agentCannotBeMovedToAMissingFacility() throws Exception {
        int a = db.addAgent("Ghost", null, null, null, db.addFacility("Rose Garden", "RG"));
        assertThrows(SQLException.class, () -> db.updateAgent(a, "Ghost", null, null, null, 99));
    }

    @Test
    void deletingFacilityWithAgentsIsRefused() throws Exception {
        int f = db.addFacility("Rose Garden", "RG");
        db.addAgent("Ghost", null, null, null, f);
        assertThrows(IllegalStateException.class, () -> db.deleteFacility(f));
        assertEquals(1, db.getFacilities().size());
    }

    @Test
    void deletingAgentOnAMissionIsRefused() throws Exception {
        createMissionsTable();
        int f = db.addFacility("Rose Garden", "RG");
        int a = db.addAgent("Ghost", null, null, null, f);
        db.assignMission(1, f, List.of(a));
        assertThrows(IllegalStateException.class, () -> db.deleteAgent(a));
        assertThrows(IllegalStateException.class, () -> db.deleteFacility(f));
        assertEquals(1, db.getAgents().size());
    }

    @Test
    void deletesWorkWhenNothingDependsOnTheRow() throws Exception {
        int f = db.addFacility("Rose Garden", "RG");
        int a = db.addAgent("Ghost", null, null, null, f);
        int b = db.addBrief("Op", "2026-01-01", "text");
        assertTrue(db.deleteBrief(b));
        assertTrue(db.deleteAgent(a));
        assertTrue(db.deleteFacility(f));
        assertFalse(db.deleteBrief(b));
        assertFalse(db.deleteAgent(a));
        assertFalse(db.deleteFacility(f));
    }

    @Test
    void otherDatabaseErrorsArePassedOn() throws Exception {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("DROP TABLE agents");
        }
        assertThrows(SQLException.class, () -> db.deleteAgent(1));
    }

    @Test
    void importLoadsSampleDataOnceAndGivesEveryAgentAFacility() throws Exception {
        Path data = Path.of("database");
        int added = db.importSampleData(data);
        assertEquals(6 + 10 + 100, added);
        assertEquals("Blackwood Annex", db.getFacilities().get(0).getName());
        assertEquals("BA", db.getFacilities().get(0).getAbbreviation());
        assertEquals("Vanguard", db.getAgents().get(0).getName());
        assertEquals("June 14, 1985", db.getAgents().get(0).getDateOfBirth());
        assertEquals(1, db.getAgents().get(0).getFacilityId());
        assertEquals(1, db.getAgents().get(6).getFacilityId());
        assertEquals("Operation Sandtrap", db.getBriefs().get(0).getTitle());

        assertEquals(0, db.importSampleData(data)); // second run adds nothing
    }

    @Test
    void importSkipsMissingFilesAndBadLines() throws Exception {
        assertEquals(0, db.importSampleData(dir));                // no files at all
        Files.writeString(dir.resolve("agents.tsv"), "Ghost\tNovember 22, 1992\n");
        assertEquals(0, db.importSampleData(dir));                // agents need a facility first
        Files.writeString(dir.resolve("facilities.tsv"), "Rose Garden\tRG\nbroken line\n\tNO\n");
        assertEquals(2, db.importSampleData(dir));                // 1 facility + 1 agent
        assertEquals(1, db.getFacilities().size());
    }

    @Test
    void unreadableSampleFileThrows() throws Exception {
        Files.createDirectory(dir.resolve("facilities.tsv"));
        assertThrows(IllegalStateException.class, () -> db.importSampleData(dir));
    }
}
