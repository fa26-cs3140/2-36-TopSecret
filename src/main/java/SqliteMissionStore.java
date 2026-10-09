import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

/**
 * SQLite implementation of MissionStore.
 * This is the ONLY class in the project that uses java.sql.
 */
public class SqliteMissionStore implements MissionStore {

    // The database lives in database/ (not data/ or ciphers/)
    public static final Path DEFAULT_DB_FILE = Path.of("database", "missions.db");
    public static final Path DEFAULT_TSV_FILE = Path.of("database", "mission_briefs.tsv");

    private static final String SELECT = "SELECT id, title, brief, date FROM missions";

    private final String url;

    // creates the table if needed and loads the TSV. Safe to run every time.
    public SqliteMissionStore() {
        this(DEFAULT_DB_FILE);
        if (Files.exists(DEFAULT_TSV_FILE)) {
            importFromTsv(DEFAULT_TSV_FILE);
        }
    }

    // Uses the given database file (created if missing) and creates the table if needed.
    public SqliteMissionStore(Path dbFile) {
        Path file = dbFile.toAbsolutePath();
        url = "jdbc:sqlite:" + file;
        try {
            Files.createDirectories(file.getParent());
            try (Connection conn = DriverManager.getConnection(url);
                 Statement st = conn.createStatement()) {
                // UNIQUE (title, date) stops the same mission being added twice
                st.execute("CREATE TABLE IF NOT EXISTS missions ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "title TEXT NOT NULL, "
                        + "brief TEXT NOT NULL, "
                        + "date TEXT NOT NULL, "
                        + "UNIQUE (title, date))");
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Cannot set up the mission database", e);
        }
    }

    @Override
    public List<Mission> getAllMissions() {
        return query(SELECT + " ORDER BY id", null);
    }

    @Override
    public Mission getMission(int id) {
        List<Mission> found = query(SELECT + " WHERE id = ?", id);
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public List<Mission> searchBriefs(String term) {
        if (term == null || term.isBlank()) {
            return new ArrayList<>();
        }
        // instr() is a plain "contains" test, so % and _ are treated as normal characters
        return query(SELECT + " WHERE instr(LOWER(brief), LOWER(?)) > 0 ORDER BY id", term.trim());
    }

    @Override
    public int importFromTsv(Path tsvFile) {
        int added = 0;
        String sql = "INSERT OR IGNORE INTO missions (title, brief, date) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false); // one save at the end is much faster
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Mission m : readTsv(tsvFile)) {
                    ps.setString(1, m.getTitle());
                    ps.setString(2, m.getBrief());
                    ps.setString(3, m.getDate());
                    added += ps.executeUpdate(); // 1 = added, 0 = already there
                }
            }
            conn.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot import missions", e);
        }
        return added;
    }

    // Runs a SELECT (with at most one ? parameter) and turns every row into a Mission.
    private List<Mission> query(String sql, Object parameter) {
        List<Mission> result = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setObject(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Mission(rs.getInt("id"), rs.getString("title"),
                            rs.getString("brief"), rs.getString("date")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read missions", e);
        }
        return result;
    }

    // Reads Title Date Text lines with tabs between. Skips the header, blank lines and bad lines.
    static List<Mission> readTsv(Path file) {
        List<Mission> missions = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t", -1);
                if (parts.length != 3 || line.contains("Title\tDate")) {
                    continue; // header, blank line, or wrong number of columns
                }
                String title = parts[0].trim();
                String date = parts[1].trim();
                String brief = parts[2].trim();
                if (!title.isEmpty() && !date.isEmpty() && !brief.isEmpty()) {
                    missions.add(new Mission(0, title, brief, date)); // database assigns the id
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read import file: " + file, e);
        }
        return missions;
    }
}