package model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Search across every text field of every record type (Member D):
// missions, briefs, agents, and facilities.
public class RecordSearch {

    public static final String MISSION = "Mission";
    public static final String BRIEF = "Brief";
    public static final String AGENT = "Agent";
    public static final String FACILITY = "Facility";

    private final ModelDatabase database;

    public RecordSearch(ModelDatabase database) {
        this.database = database;
    }

    // Records with any text field containing the term, ignoring case.
    // Ordered missions, briefs, agents, facilities, each by id.
    // A null or blank term gives an empty list.
    public List<SearchResult> search(String term) throws SQLException {
        List<SearchResult> results = new ArrayList<>();
        if (term == null || term.isBlank()) {
            return results;
        }
        String needle = term.trim().toLowerCase();
        for (Mission m : database.getMissions()) {
            if (matches(needle, m.getTitle(), m.getBrief(), m.getDate())) {
                results.add(new SearchResult(MISSION, m.getId(), m.getTitle()));
            }
        }
        for (Brief b : database.getBriefs()) {
            if (matches(needle, b.getTitle(), b.getDate(), b.getText())) {
                results.add(new SearchResult(BRIEF, b.getId(), b.getTitle()));
            }
        }
        for (Agent a : database.getAgents()) {
            if (matches(needle, a.getName(), a.getDateOfBirth(), a.getDateOfDeath(), a.getNotes())) {
                results.add(new SearchResult(AGENT, a.getId(), a.getName()));
            }
        }
        for (Facility f : database.getFacilities()) {
            if (matches(needle, f.getName(), f.getAbbreviation())) {
                results.add(new SearchResult(FACILITY, f.getId(), f.getName()));
            }
        }
        return results;
    }

    // True if any field contains the (already lowercase) term. Empty fields are skipped.
    private static boolean matches(String needle, String... fields) {
        for (String field : fields) {
            if (field != null && field.toLowerCase().contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
