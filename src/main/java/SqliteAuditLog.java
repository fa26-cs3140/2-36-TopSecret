import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// Audit log stored in the SQLite database (Member D).
public class SqliteAuditLog implements AuditLog {

    private final String url;
    private final Clock clock;

    public SqliteAuditLog(Path dbFile) {
        this(dbFile, Clock.systemDefaultZone());
    }

    // The clock can be fixed in tests so timestamps are predictable
    public SqliteAuditLog(Path dbFile, Clock clock) {
        Path file = dbFile.toAbsolutePath();
        this.url = "jdbc:sqlite:" + file;
        this.clock = clock;
        try {
            Files.createDirectories(file.getParent());
            try (Connection conn = DriverManager.getConnection(url);
                 Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS audit_log ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "timestamp TEXT NOT NULL, "
                        + "username TEXT NOT NULL, "
                        + "action TEXT NOT NULL, "
                        + "item TEXT NOT NULL)");
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Cannot set up the audit log", e);
        }
    }

    @Override
    public void record(String username, String action, String item) {
        if (isBlank(username) || isBlank(action) || isBlank(item)) {
            throw new IllegalArgumentException("Audit events need a user, an action, and an item");
        }
        String time = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS).toString();
        String sql = "INSERT INTO audit_log (timestamp, username, action, item) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, time);
            ps.setString(2, username);
            ps.setString(3, action);
            ps.setString(4, item);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot write to the audit log", e);
        }
    }

    @Override
    public List<AuditEntry> getEntries() {
        List<AuditEntry> entries = new ArrayList<>();
        String sql = "SELECT id, timestamp, username, action, item FROM audit_log ORDER BY id";
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                entries.add(new AuditEntry(rs.getInt("id"), rs.getString("timestamp"),
                        rs.getString("username"), rs.getString("action"), rs.getString("item")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read the audit log", e);
        }
        return entries;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
