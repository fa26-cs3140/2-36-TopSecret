package model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Finds single records and the records related to them (Member D).
// The web view uses this to show clickable related items: a facility's country,
// agents and missions, an agent's missions, a mission's briefs, and so on.
public class RelatedRecords {

    private final ModelDatabase database;

    public RelatedRecords(ModelDatabase database) {
        this.database = database;
    }

    // ---------- One record by id (null if there is none) ----------

    public Facility getFacility(int id) throws SQLException {
        for (Facility f : database.getFacilities()) {
            if (f.getId() == id) {
                return f;
            }
        }
        return null;
    }

    public Agent getAgent(int id) throws SQLException {
        for (Agent a : database.getAgents()) {
            if (a.getId() == id) {
                return a;
            }
        }
        return null;
    }

    public Brief getBrief(int id) throws SQLException {
        for (Brief b : database.getBriefs()) {
            if (b.getId() == id) {
                return b;
            }
        }
        return null;
    }

    // ---------- Related records ----------

    // Agents who belong to this facility
    public List<Agent> getAgentsForFacility(int facilityId) throws SQLException {
        List<Agent> result = new ArrayList<>();
        for (Agent a : database.getAgents()) {
            if (a.getFacilityId() == facilityId) {
                result.add(a);
            }
        }
        return result;
    }

    // Missions run by this facility
    public List<Mission> getMissionsForFacility(int facilityId) throws SQLException {
        List<Mission> result = new ArrayList<>();
        for (Mission m : database.getMissions()) {
            if (m.getFacilityId() == facilityId) {
                result.add(m);
            }
        }
        return result;
    }

    // Facilities assigned to this country
    public List<Facility> getFacilitiesForCountry(int countryId) throws SQLException {
        List<Facility> result = new ArrayList<>();
        for (Facility f : database.getFacilities()) {
            if (f.getCountryId() == countryId) {
                result.add(f);
            }
        }
        return result;
    }

    // The mission this brief is attached to, or null. A brief is on at most one mission.
    public Mission getMissionForBrief(int briefId) throws SQLException {
        for (Mission m : database.getMissions()) {
            if (m.getBriefIds().contains(briefId)) {
                return m;
            }
        }
        return null;
    }
}
