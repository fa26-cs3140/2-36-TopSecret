
import java.sql.*;
import java.util.*;

public class ModelDatabase {
    private final Connection connection;

    public ModelDatabase(Connection connection) throws SQLException {
        this.connection = Objects.requireNonNull(connection);

        if (!connection.getAutoCommit()) {
            throw new SQLException(
                    "Connection must start in auto-commit mode");
        }

        try (Statement s = connection.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
        }
    }

    public void createTables() throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS facilities (" +
                            "id INTEGER PRIMARY KEY, " +
                            "name TEXT NOT NULL, " +
                            "abbreviation TEXT NOT NULL)");

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS agents (" +
                            "id INTEGER PRIMARY KEY, " +
                            "name TEXT NOT NULL, " +
                            "date_of_birth TEXT, " +
                            "date_of_death TEXT, " +
                            "notes TEXT, " +
                            "facility_id INTEGER NOT NULL " +
                            "REFERENCES facilities(id) ON DELETE RESTRICT)");

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS briefs (" +
                            "id INTEGER PRIMARY KEY, " +
                            "title TEXT NOT NULL, " +
                            "date TEXT NOT NULL, " +
                            "text TEXT NOT NULL)");

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS mission_facilities (" +
                            "mission_id INTEGER PRIMARY KEY " +
                            "REFERENCES missions(id) ON DELETE CASCADE, " +
                            "facility_id INTEGER NOT NULL " +
                            "REFERENCES facilities(id) ON DELETE RESTRICT)");

            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS mission_agents (" +
                            "mission_id INTEGER NOT NULL " +
                            "REFERENCES missions(id) ON DELETE CASCADE, " +
                            "agent_id INTEGER NOT NULL " +
                            "REFERENCES agents(id) ON DELETE RESTRICT, " +
                            "PRIMARY KEY(mission_id, agent_id))");
        }
    }

    private int getLastInsertId() throws SQLException {
        try (Statement s = connection.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT last_insert_rowid()")) {
            if (r.next()) {
                return r.getInt(1);
            }
        }
        throw new SQLException("No ID returned");
    }

    public int addFacility(String name, String abbreviation)
            throws SQLException {
        try (PreparedStatement p = connection.prepareStatement(
                "INSERT INTO facilities(name, abbreviation) " +
                        "VALUES (?, ?)")) {
            p.setString(1, name);
            p.setString(2, abbreviation);
            p.executeUpdate();
        }
        return getLastInsertId();
    }

    public int addAgent(String name, String dob, String dod,
                        String notes, int facilityId)
            throws SQLException {
        try (PreparedStatement p = connection.prepareStatement(
                "INSERT INTO agents(" +
                        "name,date_of_birth,date_of_death,notes,facility_id" +
                        ") VALUES (?,?,?,?,?)")) {
            p.setString(1, name);
            p.setString(2, dob);
            p.setString(3, dod);
            p.setString(4, notes);
            p.setInt(5, facilityId);
            p.executeUpdate();
        }
        return getLastInsertId();
    }

    public int addBrief(String title, String date, String text)
            throws SQLException {
        try (PreparedStatement p = connection.prepareStatement(
                "INSERT INTO briefs(title,date,text) VALUES (?,?,?)")) {
            p.setString(1, title);
            p.setString(2, date);
            p.setString(3, text);
            p.executeUpdate();
        }
        return getLastInsertId();
    }

    public List<Facility> getFacilities() throws SQLException {
        List<Facility> result = new ArrayList<>();

        try (Statement s = connection.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT id,name,abbreviation FROM facilities ORDER BY id")) {
            while (r.next()) {
                result.add(new Facility(
                        r.getInt("id"),
                        r.getString("name"),
                        r.getString("abbreviation")));
            }
        }
        return result;
    }

    public List<Agent> getAgents() throws SQLException {
        List<Agent> result = new ArrayList<>();

        try (Statement s = connection.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT * FROM agents ORDER BY id")) {
            while (r.next()) {
                result.add(new Agent(
                        r.getInt("id"),
                        r.getString("name"),
                        r.getString("date_of_birth"),
                        r.getString("date_of_death"),
                        r.getString("notes"),
                        r.getInt("facility_id")));
            }
        }
        return result;
    }

    public List<Brief> getBriefs() throws SQLException {
        List<Brief> result = new ArrayList<>();

        try (Statement s = connection.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT * FROM briefs ORDER BY id")) {
            while (r.next()) {
                result.add(new Brief(
                        r.getInt("id"),
                        r.getString("title"),
                        r.getString("date"),
                        r.getString("text")));
            }
        }
        return result;
    }

    public void assignMission(int missionId, int facilityId,
                              List<Integer> agentIds)
            throws SQLException {


        if (agentIds == null || agentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one agent required");
        }

        for (Integer agentId : agentIds) {
            if (agentId == null) {
                throw new IllegalArgumentException(
                        "Agent IDs cannot be null");
            }
        }


        if (!connection.getAutoCommit()) {
            throw new SQLException(
                    "assignMission requires auto-commit mode");
        }

        connection.setAutoCommit(false);

        try {
            try (PreparedStatement p = connection.prepareStatement(
                    "INSERT INTO mission_facilities" +
                            "(mission_id,facility_id) VALUES (?,?)")) {
                p.setInt(1, missionId);
                p.setInt(2, facilityId);
                p.executeUpdate();
            }

            try (PreparedStatement p = connection.prepareStatement(
                    "INSERT INTO mission_agents" +
                            "(mission_id,agent_id) VALUES (?,?)")) {

                for (int agentId : new LinkedHashSet<>(agentIds)) {
                    p.setInt(1, missionId);
                    p.setInt(2, agentId);
                    p.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}


