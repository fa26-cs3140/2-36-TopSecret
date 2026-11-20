package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

// Web sessions saved in the SQLite database (Member D), so the terminal
// can list and delete sessions while the web server runs separately.
public class SqliteSessionStore implements SessionStore {

    private final String url;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SqliteSessionStore(Path dbFile) {
        this(dbFile, Clock.systemDefaultZone());
    }

    // The clock can be fixed in tests to check the 30 minute timeout
    public SqliteSessionStore(Path dbFile, Clock clock) {
        Path file = dbFile.toAbsolutePath();
        this.url = "jdbc:sqlite:" + file;
        this.clock = clock;
        try {
            Files.createDirectories(file.getParent());
            try (Connection conn = DriverManager.getConnection(url);
                 Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS web_sessions ("
                        + "token TEXT PRIMARY KEY, "
                        + "username TEXT NOT NULL, "
                        + "last_activity INTEGER NOT NULL)");
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Cannot set up the session table", e);
        }
    }

    @Override
    public String create(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("A session needs a username");
        }
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        update("INSERT INTO web_sessions (token, username, last_activity) VALUES (?, ?, ?)",
                token, username, clock.millis());
        return token;
    }

    @Override
    public String touch(String token) {
        if (token == null) {
            return null;
        }
        removeExpired();
        String username = findUsername(token);
        if (username != null) {
            update("UPDATE web_sessions SET last_activity = ? WHERE token = ?",
                    clock.millis(), token);
        }
        return username;
    }

    @Override
    public List<WebSession> list() {
        removeExpired();
        List<WebSession> sessions = new ArrayList<>();
        String sql = "SELECT token, username, last_activity FROM web_sessions "
                + "ORDER BY last_activity, rowid";
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                sessions.add(new WebSession(rs.getString("username"), rs.getString("token"),
                        toLocalTime(rs.getLong("last_activity"))));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read web sessions", e);
        }
        return sessions;
    }

    @Override
    public boolean delete(String token) {
        if (token == null) {
            return false;
        }
        return update("DELETE FROM web_sessions WHERE token = ?", token) > 0;
    }

    // A session ends once it has been idle for the full timeout
    private void removeExpired() {
        long cutoff = clock.millis() - TIMEOUT.toMillis();
        update("DELETE FROM web_sessions WHERE last_activity <= ?", cutoff);
    }

    private String findUsername(String token) {
        String sql = "SELECT username FROM web_sessions WHERE token = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("username") : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read web sessions", e);
        }
    }

    private int update(String sql, Object... values) {
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot update web sessions", e);
        }
    }

    private LocalDateTime toLocalTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), clock.getZone());
    }
}
