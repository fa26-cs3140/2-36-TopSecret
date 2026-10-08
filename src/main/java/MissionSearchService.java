import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Implements the search functionality and case-insensitive searches
public class MissionSearchService implements MissionSearch {
    private final Connection connection;

    public MissionSearchService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Mission> searchBriefs(String query) {
        List<Mission> results = new ArrayList<>();
// Return empty list immediately if the search query is null or blank
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String sql = "SELECT id, title, brief, date FROM missions WHERE LOWER(brief) LIKE LOWER(?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + query.trim() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Mission(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("brief"),
                            rs.getString("date")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Search database query failed: " + e.getMessage());
        }

        return results;
    }

    public String formatSearchResults(String query, List<Mission> matches) {
        if (matches == null || matches.isEmpty()) {
            return "No matches found for: \"" + query + "\"";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Matches found (").append(matches.size()).append("):\n");
        for (Mission m : matches) {
            sb.append("[").append(m.getId()).append("] ").append(m.getTitle()).append("\n");
        }
        return sb.toString();
    }
}