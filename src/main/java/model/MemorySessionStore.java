package model;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Web sessions kept in memory (Member D). Same rules as SqliteSessionStore,
// but nothing is saved, so it is used when no database is available (tests).
public class MemorySessionStore implements SessionStore {

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    // token -> session, in the order the sessions were created
    private final Map<String, WebSession> sessions = new LinkedHashMap<>();
    private final Map<String, Long> lastActivity = new LinkedHashMap<>();

    public MemorySessionStore() {
        this(Clock.systemDefaultZone());
    }

    // The clock can be fixed in tests to check the 30 minute timeout
    public MemorySessionStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public synchronized String create(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("A session needs a username");
        }
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        save(token, username);
        return token;
    }

    @Override
    public synchronized String touch(String token) {
        removeExpired();
        WebSession session = token == null ? null : sessions.get(token);
        if (session == null) {
            return null;
        }
        save(token, session.getUsername());
        return session.getUsername();
    }

    @Override
    public synchronized List<WebSession> list() {
        removeExpired();
        List<WebSession> result = new ArrayList<>(sessions.values());
        result.sort((a, b) -> Long.compare(
                lastActivity.get(a.getToken()), lastActivity.get(b.getToken())));
        return result;
    }

    @Override
    public synchronized boolean delete(String token) {
        lastActivity.remove(token);
        return sessions.remove(token) != null;
    }

    private void save(String token, String username) {
        long now = clock.millis();
        lastActivity.put(token, now);
        sessions.put(token, new WebSession(username, token,
                LocalDateTime.ofInstant(Instant.ofEpochMilli(now), clock.getZone())));
    }

    // A session ends once it has been idle for the full timeout
    private void removeExpired() {
        long cutoff = clock.millis() - TIMEOUT.toMillis();
        lastActivity.entrySet().removeIf(entry -> entry.getValue() <= cutoff);
        sessions.keySet().retainAll(lastActivity.keySet());
    }
}
