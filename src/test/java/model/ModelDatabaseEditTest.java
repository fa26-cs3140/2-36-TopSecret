package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

// The create/update methods added for the HW7 web forms (Member D)
class ModelDatabaseEditTest {

    private Connection connection;
    private ModelDatabase db;
    private int langley;
    private int harbor;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new ModelDatabase(connection);
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE missions (id INTEGER PRIMARY KEY, title TEXT, "
                    + "brief TEXT, date TEXT, UNIQUE(title, date))");
        }
        db.createTables();
        langley = db.addFacility("Langley Station", "LGS");
        harbor = db.addFacility("Harbor Outpost", "HBO");
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void addCountryReturnsItsId() throws Exception {
        int id = db.addCountry("Norway");

        assertEquals("Norway", db.getCountry(id).getName());
    }

    @Test
    void countryNamesAreUniqueIgnoringCase() throws Exception {
        db.addCountry("Norway");

        assertThrows(SQLException.class, () -> db.addCountry("NORWAY"));
    }

    @Test
    void updateCountryRenamesItForItsFacilities() throws Exception {
        int id = db.addCountry("Burma");
        db.setFacilityCountry(langley, id);

        assertTrue(db.updateCountry(id, "Myanmar"));

        assertEquals("Myanmar", db.getCountryForFacility(langley).getName());
    }

    @Test
    void updateCountryReturnsFalseWhenMissing() throws Exception {
        assertFalse(db.updateCountry(999, "Nowhere"));
    }

    @Test
    void setFacilityCountryAssignsAndChangesTheCountry() throws Exception {
        int norway = db.addCountry("Norway");
        int chile = db.addCountry("Chile");

        assertTrue(db.setFacilityCountry(langley, norway));
        assertEquals("Norway", db.getCountryForFacility(langley).getName());

        assertTrue(db.setFacilityCountry(langley, chile));
        assertEquals("Chile", db.getCountryForFacility(langley).getName());
        assertNull(db.getCountryForFacility(harbor));
    }

    @Test
    void countryZeroClearsTheCountry() throws Exception {
        db.setFacilityCountry(langley, db.addCountry("Norway"));

        assertTrue(db.setFacilityCountry(langley, 0));

        assertNull(db.getCountryForFacility(langley));
    }

    @Test
    void setFacilityCountryRejectsUnknownCountryAndFacility() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> db.setFacilityCountry(langley, 999));
        assertFalse(db.setFacilityCountry(999, db.addCountry("Norway")));
        assertFalse(db.setFacilityCountry(999, 0));
    }

    @Test
    void updateMissionChangesFieldsAndFacility() throws Exception {
        int id = db.addMission("Skyfall", "Recover the drive", "2010-11-12", langley);

        assertTrue(db.updateMission(id, "  Skyfall II ", "New plan", "2011-01-01", harbor));

        Mission m = db.getMissions().get(0);
        assertEquals("Skyfall II", m.getTitle());
        assertEquals("New plan", m.getBrief());
        assertEquals("2011-01-01", m.getDate());
        assertEquals(harbor, m.getFacilityId());
    }

    @Test
    void updateMissionKeepsAgentsAndBriefs() throws Exception {
        int id = db.addMission("Skyfall", "Recover the drive", "2010-11-12", langley);
        int agent = db.addAgent("Ghost", null, null, null, langley);
        int brief = db.addBrief("Report", "2010-11-13", "Text");
        db.assignAgent(id, agent);
        db.attachBrief(id, brief);

        db.updateMission(id, "Skyfall", null, "2010-11-12", harbor);

        Mission m = db.getMissions().get(0);
        assertEquals("", m.getBrief());
        assertTrue(m.hasAgent(agent));
        assertTrue(m.getBriefIds().contains(brief));
    }

    @Test
    void updateMissionRejectsBadInput() throws Exception {
        int id = db.addMission("Skyfall", "Recover the drive", "2010-11-12", langley);

        assertFalse(db.updateMission(999, "Other", "b", "2010-01-01", langley));
        assertThrows(IllegalArgumentException.class,
                () -> db.updateMission(id, " ", "b", "2010-01-01", langley));
        assertThrows(IllegalArgumentException.class,
                () -> db.updateMission(id, null, "b", "2010-01-01", langley));
        assertThrows(IllegalArgumentException.class,
                () -> db.updateMission(id, "Skyfall", "b", "2010-01-01", 999));
        assertEquals(langley, db.getMissions().get(0).getFacilityId());
    }
}
