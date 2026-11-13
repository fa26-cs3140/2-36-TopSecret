
package model;

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

// Messages for agents stored in SQLite.
// Keeps existing Member D functionality and adds
// read/unread support for HW7 Part C.
public class SqliteMessageStore implements MessageStore {

    private final String url;
    private final Clock clock;

    public SqliteMessageStore(Path dbFile) {
        this(dbFile, Clock.systemDefaultZone());
    }

    // Clock can be fixed in tests.
    public SqliteMessageStore(Path dbFile, Clock clock) {
        Path file = dbFile.toAbsolutePath();
        this.url = "jdbc:sqlite:" + file;
        this.clock = clock;

        try {
            Files.createDirectories(file.getParent());

            try (Connection conn = open();
                 Statement st = conn.createStatement()) {

                st.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        agent_id INTEGER NOT NULL
                            REFERENCES agents(id) ON DELETE CASCADE,
                        sender TEXT NOT NULL,
                        text TEXT NOT NULL,
                        sent_at TEXT NOT NULL,
                        is_read INTEGER NOT NULL DEFAULT 0
                    )
                    """);

                // Add the read column to existing databases.
                boolean hasReadColumn = false;

                try (ResultSet rs = st.executeQuery(
                        "PRAGMA table_info(messages)")) {

                    while (rs.next()) {
                        if ("is_read".equals(rs.getString("name"))) {
                            hasReadColumn = true;
                        }
                    }
                }

                if (!hasReadColumn) {
                    st.execute("""
                        ALTER TABLE messages
                        ADD COLUMN is_read INTEGER NOT NULL DEFAULT 0
                        """);
                }
            }

        } catch (IOException | SQLException e) {
            throw new IllegalStateException(
                    "Cannot set up messages", e);
        }
    }

    @Override
    public Message send(String sender, String agentName, String text) {

        if (sender == null || sender.isBlank()) {
            throw new IllegalArgumentException(
                    "A message needs a sender");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Message text cannot be empty");
        }

        String time = LocalDateTime.now(clock)
                .truncatedTo(ChronoUnit.SECONDS)
                .toString();

        try (Connection conn = open()) {

            int agentId = -1;
            String name = null;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name FROM agents " +
                            "WHERE LOWER(name) = LOWER(?) " +
                            "ORDER BY id LIMIT 1")) {

                ps.setString(1,
                        agentName == null ? "" : agentName.trim());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        agentId = rs.getInt("id");
                        name = rs.getString("name");
                    }
                }
            }

            if (name == null) {
                throw new IllegalArgumentException(
                        "No agent named " + agentName);
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO messages
                        (agent_id, sender, text, sent_at, is_read)
                    VALUES (?, ?, ?, ?, 0)
                    """)) {

                ps.setInt(1, agentId);
                ps.setString(2, sender);
                ps.setString(3, text.trim());
                ps.setString(4, time);
                ps.executeUpdate();
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT last_insert_rowid()")) {

                rs.next();

                return new Message(
                        rs.getInt(1),
                        name,
                        sender,
                        text.trim(),
                        time,
                        false
                );
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot save the message", e);
        }
    }

    @Override
    public List<Message> getMessagesFor(String username) {

        List<Message> messages = new ArrayList<>();

        if (username == null || username.isBlank()) {
            return messages;
        }

        String sql = """
            SELECT m.id, a.name, m.sender,
                   m.text, m.sent_at, m.is_read
            FROM messages m
            JOIN agents a ON a.id = m.agent_id
            WHERE LOWER(a.name) = LOWER(?)
            ORDER BY m.id
            """;

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(readMessage(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot read messages", e);
        }

        return messages;
    }

    // Retrieve a single message belonging to the user.
    @Override
    public Message getMessage(int messageId, String username) {

        if (username == null || username.isBlank()) {
            return null;
        }

        String sql = """
            SELECT m.id, a.name, m.sender,
                   m.text, m.sent_at, m.is_read
            FROM messages m
            JOIN agents a ON a.id = m.agent_id
            WHERE m.id = ?
              AND LOWER(a.name) = LOWER(?)
            """;

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, messageId);
            ps.setString(2, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readMessage(rs);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot retrieve message", e);
        }

        return null;
    }

    // Persist read status for the intended recipient only.
    @Override
    public void markAsRead(int messageId, String username) {

        if (username == null || username.isBlank()) {
            return;
        }

        String sql = """
            UPDATE messages
            SET is_read = 1
            WHERE id = ?
              AND agent_id IN (
                  SELECT id FROM agents
                  WHERE LOWER(name) = LOWER(?)
              )
            """;

        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, messageId);
            ps.setString(2, username.trim());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot mark message as read", e);
        }
    }

    // Converts a database row into a Message object.
    private Message readMessage(ResultSet rs)
            throws SQLException {

        return new Message(
                rs.getInt(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getInt(6) == 1
        );
    }

    // Foreign keys are enabled on every connection.
    private Connection open() throws SQLException {

        Connection conn = DriverManager.getConnection(url);

        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }

        return conn;
    }
}