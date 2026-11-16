package model;

import java.time.LocalDateTime;

// One logged-in web session (Member D): who it belongs to, the token
// stored in their browser cookie, and when they last made a request.
public class WebSession {

    private final String username;
    private final String token;
    private final LocalDateTime lastActivity;

    public WebSession(String username, String token, LocalDateTime lastActivity) {
        this.username = username;
        this.token = token;
        this.lastActivity = lastActivity;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
}
