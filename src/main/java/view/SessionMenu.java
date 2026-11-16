package view;

import model.AuditLog;
import model.SessionStore;

import java.io.PrintStream;
import java.util.Scanner;

// Terminal access control for web sessions (Member D), admins only.
// Lists every active web session (user, token, last activity) and lets the
// admin delete one, which forces that browser to log in again.
// Stub: methods are implemented in a later commit.
public class SessionMenu {

    public SessionMenu(SessionStore sessions, AuditLog audit, String adminUsername,
                       Scanner in, PrintStream out) {
    }

    // Menu loop: 1 list sessions, 2 delete a session, 0 back
    public void run() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // Prints one numbered line per session: user, token, last activity
    void listSessions() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // Asks for a session number (or a full token) and deletes it
    void deleteSession() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
