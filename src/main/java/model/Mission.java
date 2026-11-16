package model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

// Mission record retrieved from the SQLite database.
// A Mission is run by one Facility, has zero or more Briefs, and has
// any number of Agents (joint operations: agents may come from any Facility).
// A Mission object is a snapshot. Changes go through ModelDatabase and are reloaded.
public class Mission {
    private final int id;
    private final String title;
    private final String brief;
    private final String date;
    private final int facilityId;
    private final List<Integer> agentIds;
    private final List<Integer> briefIds;

    // Original Homework 4 constructor
    public Mission(int id, String title,
                   String brief, String date) {
        this.id = id;
        this.title = title;
        this.brief = brief;
        this.date = date;
        this.facilityId = 0;
        this.agentIds = new ArrayList<>();
        this.briefIds = new ArrayList<>();
    }

    // Homework 5 constructor
    public Mission(int id, String title,
                   String brief, String date,
                   int facilityId,
                   List<Integer> agentIds) {

        if (facilityId <= 0) {
            throw new IllegalArgumentException(
                    "Mission must have a facility");
        }

        if (agentIds == null || agentIds.isEmpty()
                || hasNull(agentIds)) {
            throw new IllegalArgumentException(
                    "Mission must have at least one agent");
        }

        this.id = id;
        this.title = title;
        this.brief = brief;
        this.date = date;
        this.facilityId = facilityId;
        this.agentIds = new ArrayList<>(agentIds);
        this.briefIds = new ArrayList<>();
    }

    // Constructor used when loading from the database.
    // A mission may have no agents yet and no briefs. facilityId 0 means
    // "no facility recorded" (missions loaded without a facility row).
    public Mission(int id, String title,
                   String brief, String date,
                   int facilityId,
                   List<Integer> agentIds,
                   List<Integer> briefIds) {

        if (facilityId < 0) {
            throw new IllegalArgumentException(
                    "Facility id cannot be negative");
        }

        if (agentIds == null || hasNull(agentIds)) {
            throw new IllegalArgumentException(
                    "Agent ids cannot be null");
        }

        if (briefIds == null || hasNull(briefIds)) {
            throw new IllegalArgumentException(
                    "Brief ids cannot be null");
        }

        this.id = id;
        this.title = title;
        this.brief = brief;
        this.date = date;
        this.facilityId = facilityId;
        // An agent is assigned to a mission at most once
        this.agentIds = new ArrayList<>(new LinkedHashSet<>(agentIds));
        this.briefIds = new ArrayList<>(new LinkedHashSet<>(briefIds));
    }

    // List.of(...).contains(null) throws NullPointerException, so look for null by hand
    private static boolean hasNull(List<Integer> ids) {
        for (Integer id : ids) {
            if (id == null) {
                return true;
            }
        }
        return false;
    }

    // Getter methods
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBrief() {
        return brief;
    }

    public String getDate() {
        return date;
    }

    public int getFacilityId() {
        return facilityId;
    }

    public List<Integer> getAgentIds() {
        return new ArrayList<>(agentIds);
    }

    public List<Integer> getBriefIds() {
        return new ArrayList<>(briefIds);
    }

    public boolean hasAgent(int agentId) {
        return agentIds.contains(agentId);
    }

    // Equals method from teammate's code
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Mission)) {
            return false;
        }

        Mission other = (Mission) o;

        return id == other.id
                && Objects.equals(title, other.title)
                && Objects.equals(brief, other.brief)
                && Objects.equals(date, other.date);
    }

    // HashCode method from teammate's code
    @Override
    public int hashCode() {
        return Objects.hash(id, title, brief, date);
    }

    // One plain-text line. The terminal prints it as is, and the web view can wrap it in tags.
    @Override
    public String toString() {
        return "Mission #" + id + ": " + title + " (" + date + "), "
                + (facilityId == 0 ? "no facility" : "facility " + facilityId)
                + ", " + agentIds.size() + " agent(s), "
                + briefIds.size() + " brief(s)";
    }
}