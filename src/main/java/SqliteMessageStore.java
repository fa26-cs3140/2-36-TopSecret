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

// Messages for agents stored in the SQLite database (Member D).
// Each message points at a row in the agents table, so a message can never be
// addressed to an agent that does not exist. Deleting an agent deletes their messages.
public class SqliteMessageStore implements MessageStore {

    private final String url;
    private final Clock clock;

    public SqliteMessageStore(Path dbFile) {
        this(dbFile, Clock.systemDefaultZone());
    }

    // The clock can be fixed in tests so timestamps are predictable
    public SqliteMessageStore(Path dbFile, Clock clock) {
        Path file = dbFile.toAbsolutePath();
        this.url = "jdbc:sqlite:" + file;
        this.clock = clock;
        try {
            Files.createDirectories(file.getParent());
            try (Connection conn = open(); Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS messages ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "agent_id INTEGER NOT NULL REFERENCES agents(id) ON DELETE CASCADE, "
                        + "sender TEXT NOT NULL, "
                        + "text TEXT NOT NULL, "
                        + "sent_at TEXT NOT NULL)");
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Cannot set up messages", e);
        }
    }

    @Override
    public Message send(String sender, String agentName, String text) {
        if (sender == null || sender.isBlank()) {
            throw new IllegalArgumentException("A message needs a sender");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Message text cannot be empty");
        }
        String time = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS).toString();
        try (Connection conn = open()) {
            int agentId = -1;
            String name = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name FROM agents WHERE LOWER(name) = LOWER(?) ORDER BY id LIMIT 1")) {
                ps.setString(1, agentName == null ? "" : agentName.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        agentId = rs.getInt("id");
                        name = rs.getString("name");
                    }
                }
            }
            if (name == null) {
                throw new IllegalArgumentException("No agent named " + agentName);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO messages (agent_id, sender, text, sent_at) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, agentId);
                ps.setString(2, sender);
                ps.setString(3, text.trim());
                ps.setString(4, time);
                ps.executeUpdate();
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                return new Message(rs.getInt(1), name, sender, text.trim(), time);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save the message", e);
        }
    }

    @Override
    public List<Message> getMessagesFor(String username) {
        List<Message> messages = new ArrayList<>();
        if (username == null || username.isBlank()) {
            return messages;
        }
        String sql = "SELECT m.id, a.name, m.sender, m.text, m.sent_at FROM messages m "
                + "JOIN agents a ON a.id = m.agent_id "
                + "WHERE LOWER(a.name) = LOWER(?) ORDER BY m.id";
        try (Connection conn = open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message(rs.getInt(1), rs.getString(2),
                            rs.getString(3), rs.getString(4), rs.getString(5)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read messages", e);
        }
        return messages;
    }

    // Every connection turns foreign keys on, so the agent link is enforced
    private Connection open() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
}
