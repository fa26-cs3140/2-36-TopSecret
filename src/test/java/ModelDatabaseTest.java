import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ModelDatabaseTest {
    @Test
    void storesAndReadsAllModelTypes() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = new ModelDatabase(connection);
            db.createTables();
            int facilityId = db.addFacility("Headquarters", "HQ");
            int agentId = db.addAgent("Smith", "1995-01-01", null, "Notes", facilityId);
            int briefId = db.addBrief("Operation", "2026-10-19", "Brief text");
            assertEquals(facilityId, db.getFacilities().get(0).getId());
            assertEquals(agentId, db.getAgents().get(0).getId());
            assertEquals(briefId, db.getBriefs().get(0).getId());
            assertEquals("HQ", db.getFacilities().get(0).getAbbreviation());
            assertEquals("Smith", db.getAgents().get(0).getName());
            assertEquals("Brief text", db.getBriefs().get(0).getText());
        }
    }

    @Test
    void rejectsAgentWithoutFacilityAndProtectedFacilityDeletion() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = new ModelDatabase(connection);
            db.createTables();
            assertThrows(SQLException.class, () -> db.addAgent("Unknown", null, null, null, 999));
            int facilityId = db.addFacility("Base", "B");
            db.addAgent("Smith", null, null, null, facilityId);
            try (PreparedStatement p = connection.prepareStatement("DELETE FROM facilities WHERE id=?")) {
                p.setInt(1, facilityId);
                assertThrows(SQLException.class, p::executeUpdate);
            }
        }
    }


    @Test
    void assignsMissionAndRejectsEmptyAgents() throws Exception {
        try (Connection connection =
                     DriverManager.getConnection("jdbc:sqlite::memory:")) {

            ModelDatabase db = new ModelDatabase(connection);
            db.createTables();

            // Create facility and agent
            int facilityId = db.addFacility("Base", "B");
            int agentId = db.addAgent(
                    "Smith", null, null, null, facilityId);

            // Create a mission for testing
            try (Statement s = connection.createStatement()) {
                s.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS missions (" +
                                "id INTEGER PRIMARY KEY, " +
                                "title TEXT, brief TEXT, date TEXT)");

                s.executeUpdate(
                        "INSERT INTO missions(id, title, brief, date) " +
                                "VALUES (1, 'Test Mission', " +
                                "'Test brief', '2026-10-19')");
            }

            // Check that empty agent lists are rejected
            assertThrows(
                    IllegalArgumentException.class,
                    () -> db.assignMission(
                            1, facilityId, List.of()));

            // Assign the agent to the mission
            db.assignMission(
                    1, facilityId, List.of(agentId));

            // Check mission_agents table
            try (Statement s = connection.createStatement();
                 ResultSet r = s.executeQuery(
                         "SELECT COUNT(*) FROM mission_agents")) {

                assertTrue(r.next());
                assertEquals(1, r.getInt(1));
            }
        }
    }

    // The real program gets the missions table from SqliteMissionStore
    private ModelDatabase openDb(Connection connection) throws SQLException {
        ModelDatabase db = new ModelDatabase(connection);
        db.createTables();
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS missions ("
                    + "id INTEGER PRIMARY KEY, title TEXT, brief TEXT, date TEXT)");
        }
        return db;
    }

    @Test
    void addMissionStoresFacilityAndRejectsBadInput() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int facilityId = db.addFacility("Base", "B");
            int missionId = db.addMission("Op", "brief", "2026-10-19", facilityId);

            Mission m = db.getMission(missionId);
            assertEquals("Op", m.getTitle());
            assertEquals("brief", m.getBrief());
            assertEquals("2026-10-19", m.getDate());
            assertEquals(facilityId, m.getFacilityId());
            assertTrue(m.getAgentIds().isEmpty());
            assertTrue(m.getBriefIds().isEmpty());
            assertTrue(connection.getAutoCommit());

            assertThrows(IllegalArgumentException.class, () -> db.addMission(" ", "b", "d", facilityId));
            assertThrows(IllegalArgumentException.class, () -> db.addMission(null, "b", "d", facilityId));
            assertThrows(IllegalArgumentException.class, () -> db.addMission("Op", "b", "d", 999));
            assertEquals(1, db.getMissions().size());
        }
    }

    @Test
    void assignAgentAllowsAgentsFromAnyFacility() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int baseId = db.addFacility("Base", "B");
            int otherId = db.addFacility("Other", "O");
            int smith = db.addAgent("Smith", null, null, null, baseId);
            int jones = db.addAgent("Jones", null, null, null, otherId);
            int missionId = db.addMission("Op", "b", "d", baseId);

            assertTrue(db.assignAgent(missionId, smith));
            assertTrue(db.assignAgent(missionId, jones)); // joint operation
            assertEquals(List.of(smith, jones), db.getMission(missionId).getAgentIds());

            assertFalse(db.assignAgent(missionId, smith)); // already assigned
            assertThrows(IllegalArgumentException.class, () -> db.assignAgent(999, smith));
            assertThrows(IllegalArgumentException.class, () -> db.assignAgent(missionId, 999));
        }
    }

    @Test
    void unassignAgentRemovesOnlyThatAgent() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int facilityId = db.addFacility("Base", "B");
            int smith = db.addAgent("Smith", null, null, null, facilityId);
            int jones = db.addAgent("Jones", null, null, null, facilityId);
            int missionId = db.addMission("Op", "b", "d", facilityId);
            db.assignAgent(missionId, smith);
            db.assignAgent(missionId, jones);

            assertTrue(db.unassignAgent(missionId, smith));
            assertFalse(db.unassignAgent(missionId, smith));
            assertEquals(List.of(jones), db.getMission(missionId).getAgentIds());
        }
    }

    @Test
    void attachBriefLinksOneBriefToOneMission() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int facilityId = db.addFacility("Base", "B");
            int first = db.addMission("Op", "b", "d", facilityId);
            int second = db.addMission("Op 2", "b", "d", facilityId);
            int briefId = db.addBrief("Brief", "2026-10-19", "text");

            assertTrue(db.attachBrief(first, briefId));
            assertFalse(db.attachBrief(first, briefId)); // already attached
            assertEquals(List.of(briefId), db.getMission(first).getBriefIds());

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> db.attachBrief(second, briefId));
            assertTrue(e.getMessage().contains("already belongs to mission #" + first));
            assertThrows(IllegalArgumentException.class, () -> db.attachBrief(999, briefId));
            assertThrows(IllegalArgumentException.class, () -> db.attachBrief(first, 999));
        }
    }

    @Test
    void detachBriefFreesTheBrief() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int facilityId = db.addFacility("Base", "B");
            int first = db.addMission("Op", "b", "d", facilityId);
            int second = db.addMission("Op 2", "b", "d", facilityId);
            int briefId = db.addBrief("Brief", "2026-10-19", "text");
            db.attachBrief(first, briefId);

            assertTrue(db.detachBrief(first, briefId));
            assertFalse(db.detachBrief(first, briefId));
            assertTrue(db.attachBrief(second, briefId));
        }
    }

    @Test
    void getMissionsLoadsFacilityAgentsAndBriefs() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int facilityId = db.addFacility("Base", "B");
            int agentId = db.addAgent("Smith", null, null, null, facilityId);
            int briefId = db.addBrief("Brief", "2026-10-19", "text");
            int missionId = db.addMission("Op", "b", "d", facilityId);
            db.assignAgent(missionId, agentId);
            db.attachBrief(missionId, briefId);
            try (Statement s = connection.createStatement()) {
                s.executeUpdate("INSERT INTO missions(title, brief, date) VALUES ('Old', 'text', '1970-11-03')");
            }

            List<Mission> all = db.getMissions();
            assertEquals(2, all.size());
            assertEquals(facilityId, all.get(0).getFacilityId());
            assertEquals(List.of(agentId), all.get(0).getAgentIds());
            assertEquals(List.of(briefId), all.get(0).getBriefIds());
            assertEquals("Old", all.get(1).getTitle());
            assertEquals(0, all.get(1).getFacilityId());
            assertTrue(all.get(1).getAgentIds().isEmpty());
            assertTrue(all.get(1).getBriefIds().isEmpty());
        }
    }

    @Test
    void getMissionReturnsNullForUnknownId() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int facilityId = db.addFacility("Base", "B");
            int missionId = db.addMission("Op", "b", "d", facilityId);
            assertEquals("Op", db.getMission(missionId).getTitle());
            assertNull(db.getMission(999));
        }
    }

    @Test
    void getMissionsForAgentListsEveryAssignedMission() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int facilityId = db.addFacility("Base", "B");
            int smith = db.addAgent("Smith", null, null, null, facilityId);
            int jones = db.addAgent("Jones", null, null, null, facilityId);
            int first = db.addMission("Op", "b", "d", facilityId);
            int second = db.addMission("Op 2", "b", "d", facilityId);
            db.assignAgent(first, smith);
            db.assignAgent(second, smith);

            List<Mission> missions = db.getMissionsForAgent(smith);
            assertEquals(2, missions.size());
            assertEquals(first, missions.get(0).getId());
            assertEquals(second, missions.get(1).getId());
            assertTrue(db.getMissionsForAgent(jones).isEmpty());
        }
    }

    @Test
    void getAgentsForMissionReturnsAgentRecords() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int baseId = db.addFacility("Base", "B");
            int otherId = db.addFacility("Other", "O");
            int smith = db.addAgent("Smith", null, null, null, baseId);
            int jones = db.addAgent("Jones", null, null, null, otherId);
            int missionId = db.addMission("Op", "b", "d", baseId);
            db.assignAgent(missionId, jones);
            db.assignAgent(missionId, smith);

            List<Agent> agents = db.getAgentsForMission(missionId);
            assertEquals(2, agents.size());
            assertEquals("Smith", agents.get(0).getName());
            assertEquals("Jones", agents.get(1).getName());
            assertEquals(otherId, agents.get(1).getFacilityId());
            assertTrue(db.getAgentsForMission(999).isEmpty());
        }
    }

    @Test
    void getBriefsForMissionReturnsBriefRecords() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ModelDatabase db = openDb(connection);
            int facilityId = db.addFacility("Base", "B");
            int missionId = db.addMission("Op", "b", "d", facilityId);
            assertTrue(db.getBriefsForMission(missionId).isEmpty());

            int first = db.addBrief("First", "2026-10-19", "one");
            int second = db.addBrief("Second", "2026-10-20", "two");
            db.attachBrief(missionId, first);
            db.attachBrief(missionId, second);

            List<Brief> briefs = db.getBriefsForMission(missionId);
            assertEquals(2, briefs.size());
            assertEquals("First", briefs.get(0).getTitle());
            assertEquals("two", briefs.get(1).getText());
        }
    }
}
