package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Finding one record and the records related to it (Member D)
class RelatedRecordsTest {

    private Connection connection;
    private ModelDatabase db;
    private RelatedRecords related;
    private int norway;
    private int langley;
    private int harbor;
    private int ghost;
    private int vanguard;
    private int report;
    private int skyfall;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new ModelDatabase(connection);
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE missions (id INTEGER PRIMARY KEY, title TEXT, "
                    + "brief TEXT, date TEXT)");
        }
        db.createTables();
        norway = db.addCountry("Norway");
        langley = db.addFacility("Langley Station", "LGS");
        harbor = db.addFacility("Harbor Outpost", "HBO");
        db.setFacilityCountry(langley, norway);
        ghost = db.addAgent("Ghost", null, null, null, langley);
        vanguard = db.addAgent("Vanguard", null, null, null, harbor);
        report = db.addBrief("Harbor Report", "2005-09-10", "Ships seen");
        db.addBrief("Loose Brief", "2005-09-11", "Not attached");
        skyfall = db.addMission("Skyfall", "Recover the drive", "2010-11-12", langley);
        db.addMission("Black Sky", "Watch the harbor", "2005-09-12", harbor);
        db.attachBrief(skyfall, report);
        related = new RelatedRecords(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void findsOneRecordById() throws Exception {
        assertEquals("Langley Station", related.getFacility(langley).getName());
        assertEquals("Ghost", related.getAgent(ghost).getName());
        assertEquals("Harbor Report", related.getBrief(report).getTitle());
    }

    @Test
    void missingRecordsAreNull() throws Exception {
        assertNull(related.getFacility(999));
        assertNull(related.getAgent(999));
        assertNull(related.getBrief(999));
    }

    @Test
    void agentsForFacility() throws Exception {
        List<Agent> agents = related.getAgentsForFacility(langley);

        assertEquals(1, agents.size());
        assertEquals("Ghost", agents.get(0).getName());
        assertEquals("Vanguard", related.getAgentsForFacility(harbor).get(0).getName());
        assertTrue(related.getAgentsForFacility(999).isEmpty());
    }

    @Test
    void missionsForFacility() throws Exception {
        List<Mission> missions = related.getMissionsForFacility(langley);

        assertEquals(1, missions.size());
        assertEquals("Skyfall", missions.get(0).getTitle());
        assertTrue(related.getMissionsForFacility(999).isEmpty());
    }

    @Test
    void facilitiesForCountry() throws Exception {
        List<Facility> facilities = related.getFacilitiesForCountry(norway);

        assertEquals(1, facilities.size());
        assertEquals("Langley Station", facilities.get(0).getName());
        assertTrue(related.getFacilitiesForCountry(999).isEmpty());
    }

    @Test
    void missionForBrief() throws Exception {
        assertEquals("Skyfall", related.getMissionForBrief(report).getTitle());
        assertNull(related.getMissionForBrief(report + 1)); // the loose brief
        assertNull(related.getMissionForBrief(999));
    }
}
