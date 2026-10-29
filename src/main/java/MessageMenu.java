import java.io.PrintStream;
import java.util.Scanner;

// Terminal menu for agent messages (Member D). Any logged-in user can send a
// message to an agent. A user whose username matches an agent's name (ignoring
// case) can read that agent's messages. Sending and reading are audited.
//
// STUB (week 1): the methods exist so the tests compile; they are filled in next week.
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

    // Shows: 1. Send a message to an agent, 2. Read my messages (N), 0. Back
    public void run() {
        // TODO week 2
    }

    // Shown right after login: "You have N messages. Open Messages to read them."
    public void printInboxNotice() {
        // TODO week 2
    }

    // Lists agents, lets the user pick one by number or name, saves the message, audits CREATE
    void sendMessage() {
        // TODO week 2
    }

    // Shows this user's messages and audits a READ for each
    void readMessages() {
        // TODO week 2
    }
}
