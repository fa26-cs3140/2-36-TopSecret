package model;

import java.sql.SQLException;
import java.util.List;

// Search across every text field of every record type (Member D):
// missions, briefs, agents, and facilities.
// Stub: implemented in a later commit.
public class RecordSearch {

    public static final String MISSION = "Mission";
    public static final String BRIEF = "Brief";
    public static final String AGENT = "Agent";
    public static final String FACILITY = "Facility";

    public RecordSearch(ModelDatabase database) {
    }

    // Records with any text field containing the term, ignoring case.
    // Ordered missions, briefs, agents, facilities, each by id.
    // A null or blank term gives an empty list.
    public List<SearchResult> search(String term) throws SQLException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
