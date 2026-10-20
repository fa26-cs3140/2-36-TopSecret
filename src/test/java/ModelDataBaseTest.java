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
}
