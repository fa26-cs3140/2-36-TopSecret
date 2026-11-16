package model;

import java.time.Duration;
import java.util.List;

// Web login sessions (Member D, access control).
// A session stays valid while it is in use and ends after 30 minutes
// without a request. An admin can list sessions and delete a token from
// the terminal, which forces that browser to log in again.
public interface SessionStore {

    Duration TIMEOUT = Duration.ofMinutes(30);

    // Starts a session for the user and returns its new token
    String create(String username);

    // Username for a valid token, and resets its 30 minute timer.
    // Returns null if the token is unknown, deleted, or timed out.
    String touch(String token);

    // All sessions that have not timed out, oldest activity first
    List<WebSession> list();

    // Ends the session. Returns false if the token was not found.
    boolean delete(String token);
}
