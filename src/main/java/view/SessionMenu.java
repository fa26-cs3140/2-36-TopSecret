package view;

import model.AuditLog;
import model.SessionStore;
import model.WebSession;

import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

// Terminal access control for web sessions (Member D), admins only.
// Lists every active web session (user, token, last activity) and lets the
// admin delete one, which forces that browser to log in again.
public class SessionMenu {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SessionStore sessions;
    private final AuditLog audit;
    private final String adminUsername;
    private final Scanner in;
    private final PrintStream out;

    public SessionMenu(SessionStore sessions, AuditLog audit, String adminUsername,
                       Scanner in, PrintStream out) {
        this.sessions = sessions;
        this.audit = audit;
        this.adminUsername = adminUsername;
        this.in = in;
        this.out = out;
    }

    // Menu loop: 1 list sessions, 2 delete a session, 0 back
    public void run() {
        while (true) {
            out.println("\n=== WEB SESSIONS ===");
            out.println("1. List active sessions");
            out.println("2. Delete a session");
            out.println("0. Back");
            String choice = ask("Choose an option: ");
            if (choice == null || choice.equals("0")) {
                return;
            }
            switch (choice) {
                case "1" -> listSessions();
                case "2" -> deleteSession();
                default -> out.println("Invalid choice.");
            }
        }
    }

    // Prints one numbered line per session: user, token, last activity
    void listSessions() {
        printSessions(sessions.list());
    }

    // Asks for a session number (or a full token) and deletes it
    void deleteSession() {
        List<WebSession> active = sessions.list();
        if (!printSessions(active)) {
            return;
        }
        String choice = ask("Session number or token to delete: ");
        WebSession chosen = find(active, choice);
        if (chosen == null) {
            out.println("No session matches that choice.");
            return;
        }
        sessions.delete(chosen.getToken());
        audit.record(adminUsername, AuditLog.DELETE, "Web session for " + chosen.getUsername());
        out.println("Session ended for " + chosen.getUsername() + ".");
    }

    // Returns false when there is nothing to show
    private boolean printSessions(List<WebSession> active) {
        if (active.isEmpty()) {
            out.println("No active web sessions.");
            return false;
        }
        for (int i = 0; i < active.size(); i++) {
            WebSession s = active.get(i);
            out.println((i + 1) + ". " + s.getUsername() + "  token " + s.getToken()
                    + "  last active " + s.getLastActivity().format(TIME));
        }
        return true;
    }

    private static WebSession find(List<WebSession> active, String choice) {
        if (choice == null) {
            return null;
        }
        for (int i = 0; i < active.size(); i++) {
            if (choice.equals(String.valueOf(i + 1)) || choice.equals(active.get(i).getToken())) {
                return active.get(i);
            }
        }
        return null;
    }

    private String ask(String prompt) {
        out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }
}
