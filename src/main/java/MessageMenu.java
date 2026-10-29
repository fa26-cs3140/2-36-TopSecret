import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

// Terminal menu for agent messages (Member D). Any logged-in user can send a
// message to an agent. A user whose username matches an agent's name (ignoring
// case) can read that agent's messages. Sending and reading are audited.
public class MessageMenu {

    private final MessageStore messages;
    private final ModelDatabase database;
    private final AuditLog audit;
    private final UserAccount user;
    private final Scanner in;
    private final PrintStream out;

    public MessageMenu(MessageStore messages, ModelDatabase database, AuditLog audit,
                       UserAccount user, Scanner in, PrintStream out) {
        this.messages = messages;
        this.database = database;
        this.audit = audit;
        this.user = user;
        this.in = in;
        this.out = out;
    }

    public void run() {
        while (true) {
            out.println("\n=== MESSAGES ===");
            out.println("1. Send a message to an agent");
            out.println("2. Read my messages (" + messages.getMessagesFor(user.getUsername()).size() + ")");
            out.println("0. Back");
            String choice = ask("Choose an option: ");
            if (choice == null || choice.equals("0")) {
                return;
            }
            switch (choice) {
                case "1" -> sendMessage();
                case "2" -> readMessages();
                default -> out.println("Invalid choice.");
            }
        }
    }

    // Shown right after login so agents know they have mail
    public void printInboxNotice() {
        int count = messages.getMessagesFor(user.getUsername()).size();
        if (count > 0) {
            out.println("You have " + count + (count == 1 ? " message" : " messages")
                    + ". Open Messages to read them.");
        }
    }

    void sendMessage() {
        List<Agent> agents;
        try {
            agents = database.getAgents();
        } catch (SQLException e) {
            out.println("Database error: " + e.getMessage());
            return;
        }
        if (agents.isEmpty()) {
            out.println("There are no agents to message.");
            return;
        }
        out.println("\nAgents:");
        for (Agent a : agents) {
            out.println(a.getId() + ". " + a.getName());
        }
        String choice = ask("Agent number or name: ");
        if (choice == null) {
            return;
        }
        String agentName = findAgentName(agents, choice);
        if (agentName == null) {
            out.println("No agent matches '" + choice + "'.");
            return;
        }
        String text = ask("Message to " + agentName + ": ");
        if (text == null) {
            return;
        }
        try {
            Message sent = messages.send(user.getUsername(), agentName, text);
            audit.record(user.getUsername(), AuditLog.CREATE,
                    "Message #" + sent.getId() + " to " + sent.getAgentName());
            out.println("Message sent to " + sent.getAgentName() + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    void readMessages() {
        List<Message> inbox = messages.getMessagesFor(user.getUsername());
        if (inbox.isEmpty()) {
            out.println("You have no messages.");
            return;
        }
        out.println("\nMessages for " + inbox.get(0).getAgentName() + ":");
        for (Message m : inbox) {
            out.println(m);
            audit.record(user.getUsername(), AuditLog.READ, "Message #" + m.getId());
        }
    }

    // Matches the agent's id number or name (ignoring case); null if none
    private static String findAgentName(List<Agent> agents, String choice) {
        String c = choice.trim();
        for (Agent a : agents) {
            if (String.valueOf(a.getId()).equals(c) || a.getName().equalsIgnoreCase(c)) {
                return a.getName();
            }
        }
        return null;
    }

    private String ask(String prompt) {
        out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }
}
