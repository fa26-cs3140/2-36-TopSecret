package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Creating and updating records with every change audited (Member D)
class RecordEditorTest {

    private Connection connection;
    private ModelDatabase db;
    private RecordEditor editor;
    private final List<String> audit = new ArrayList<>();
    private int langley;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new ModelDatabase(connection);
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE missions (id INTEGER PRIMARY KEY, title TEXT, "
                    + "brief TEXT, date TEXT)");
        }
        db.createTables();
        langley = db.addFacility("Langley Station", "LGS");
        editor = new RecordEditor(db, new AuditLog() {
            @Override
            public void record(String username, String action, String item) {
                audit.add(username + " " + action + " " + item);
            }

            @Override
            public List<AuditEntry> getEntries() {
                return List.of();
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void createsAndUpdatesBrief() throws Exception {
        int id = editor.saveBrief("admin", 0, " Report ", "2026-01-01", "Text");
        assertEquals("Report", db.getBriefs().get(0).getTitle());

        assertEquals(id, editor.saveBrief("admin", id, "Report v2", "2026-01-02", "New text"));

        assertEquals("Report v2", db.getBriefs().get(0).getTitle());
        assertEquals(List.of("admin CREATE Brief #" + id, "admin UPDATE Brief #" + id), audit);
    }

    @Test
    void createsAndUpdatesAgent() throws Exception {
        int id = editor.saveAgent("admin", 0, "Ghost", "1980-04-02", " ", "", langley);
        Agent created = db.getAgents().get(0);
        assertEquals("Ghost", created.getName());
        assertNull(created.getDateOfDeath());
        assertNull(created.getNotes());

        editor.saveAgent("admin", id, "Ghost", "1980-04-02", "2020-01-01", "Retired", langley);

        Agent updated = db.getAgents().get(0);
        assertEquals("2020-01-01", updated.getDateOfDeath());
        assertEquals("Retired", updated.getNotes());
        assertEquals(List.of("admin CREATE Agent #" + id, "admin UPDATE Agent #" + id), audit);
    }

    @Test
    void createsAndUpdatesFacilityWithCountry() throws Exception {
        int norway = db.addCountry("Norway");
        int chile = db.addCountry("Chile");

        int id = editor.saveFacility("admin", 0, "Oslo Station", "OSL", norway);
        assertEquals("Norway", db.getCountryForFacility(id).getName());

        editor.saveFacility("admin", id, "Santiago Station", "SCL", chile);
        assertEquals("Chile", db.getCountryForFacility(id).getName());

        editor.saveFacility("admin", id, "Santiago Station", "SCL", 0);
        assertNull(db.getCountryForFacility(id));
        assertEquals(3, audit.size());
        assertEquals("admin CREATE Facility #" + id, audit.get(0));
        assertEquals("admin UPDATE Facility #" + id, audit.get(2));
    }

    @Test
    void createsAndUpdatesCountry() throws Exception {
        int id = editor.saveCountry("admin", 0, "Burma");

        editor.saveCountry("admin", id, "Myanmar");

        assertEquals("Myanmar", db.getCountry(id).getName());
        assertEquals(List.of("admin CREATE Country #" + id, "admin UPDATE Country #" + id), audit);
    }

    @Test
    void createsAndUpdatesMission() throws Exception {
        int harbor = db.addFacility("Harbor Outpost", "HBO");

        int id = editor.saveMission("admin", 0, "Skyfall", "Recover the drive", "2010-11-12", langley);
        editor.saveMission("admin", id, "Skyfall II", "New plan", "2011-01-01", harbor);

        Mission m = db.getMissions().get(0);
        assertEquals("Skyfall II", m.getTitle());
        assertEquals(harbor, m.getFacilityId());
        assertEquals(List.of("admin CREATE Mission #" + id, "admin UPDATE Mission #" + id), audit);
    }

    @Test
    void missingRequiredFieldsAreRejectedAndNotAudited() {
        assertEquals("Title is required", assertThrows(IllegalArgumentException.class,
                () -> editor.saveBrief("admin", 0, " ", "2026-01-01", "Text")).getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveBrief("admin", 0, "Title", null, "Text"));
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveBrief("admin", 0, "Title", "2026-01-01", ""));
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveAgent("admin", 0, "", null, null, null, langley));
        assertEquals("Facility is required", assertThrows(IllegalArgumentException.class,
                () -> editor.saveAgent("admin", 0, "Ghost", null, null, null, 0)).getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveFacility("admin", 0, "Name", " ", 0));
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveCountry("admin", 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveMission("admin", 0, "Title", "Brief", "2026-01-01", 0));
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveMission("admin", 0, "Title", "", "2026-01-01", langley));
        assertTrue(audit.isEmpty());
    }

    @Test
    void updatingAMissingRecordIsRejectedAndNotAudited() {
        assertEquals("No such brief: 999", assertThrows(IllegalArgumentException.class,
                () -> editor.saveBrief("admin", 999, "T", "D", "X")).getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveAgent("admin", 999, "Ghost", null, null, null, langley));
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveFacility("admin", 999, "Name", "NM", 0));
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveCountry("admin", 999, "Nowhere"));
        assertThrows(IllegalArgumentException.class,
                () -> editor.saveMission("admin", 999, "T", "B", "D", langley));
        assertTrue(audit.isEmpty());
    }
}
