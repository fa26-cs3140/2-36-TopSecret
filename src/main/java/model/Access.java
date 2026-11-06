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
import java.util.LinkedHashMap;
import java.util.Map;

// Stores each user's access level (ADMIN or STANDARD) in the database
public class Access {

    public static final String ADMIN = "ADMIN";
    public static final String STANDARD = "STANDARD";
    public static final Path DEFAULT_DB_FILE = Path.of("database", "missions.db");

    private final String url;

    public Access() {
        this(DEFAULT_DB_FILE);
    }

    public Access(Path dbFile) {
        Path file = dbFile.toAbsolutePath();
        url = "jdbc:sqlite:" + file;
        try {
            Files.createDirectories(file.getParent());
            try (Connection conn = DriverManager.getConnection(url);
                 Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS user_access ("
                        + "username TEXT PRIMARY KEY, level TEXT NOT NULL)");
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Cannot set up the access table", e);
        }
    }

    // First user becomes ADMIN, everyone after is STANDARD
    public void registerUser(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("A username is required");
        }
        if (getLevel(username) == null) {
            String level = getAllUsers().isEmpty() ? ADMIN : STANDARD;
            runUpdate("INSERT INTO user_access (username, level) VALUES (?, ?)", username, level);
        }
    }

    // "ADMIN", "STANDARD", or null if not registered
    public String getLevel(String username) {
        String sql = "SELECT level FROM user_access WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("level") : null;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read access levels", e);
        }
    }

    public boolean isAdmin(String username) {
        return ADMIN.equals(getLevel(username));
    }

    public void promote(String actingUser, String target) {
        changeLevel(actingUser, target, STANDARD, ADMIN);
    }

    public void demote(String actingUser, String target) {
        if (target.equalsIgnoreCase(actingUser)) {
            throw new IllegalArgumentException("You cannot demote yourself");
        }
        changeLevel(actingUser, target, ADMIN, STANDARD);
    }

    // All users with their levels, oldest first
    public Map<String, String> getAllUsers() {
        Map<String, String> users = new LinkedHashMap<>();
        String sql = "SELECT username, level FROM user_access ORDER BY rowid";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.put(rs.getString("username"), rs.getString("level"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read access levels", e);
        }
        return users;
    }

    // Admin-only: moves the target from one level to another
    private void changeLevel(String actingUser, String target, String from, String to) {
        if (!isAdmin(actingUser)) {
            throw new IllegalArgumentException("Only admins can change access levels");
        }
        String level = getLevel(target);
        if (level == null) {
            throw new IllegalArgumentException("No such user: " + target);
        }
        if (!level.equals(from)) {
            throw new IllegalArgumentException(target + " is already " + to);
        }
        runUpdate("UPDATE user_access SET level = ? WHERE username = ?", to, target);
    }

    private void runUpdate(String sql, String first, String second) {
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, first);
            ps.setString(2, second);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save access level", e);
        }
    }
}