package model;

// A message left for an agent (Member D).
// Only the user whose username matches the agent's name
// (ignoring case) can read it.
public class Message {

    private final int id;
    private final String agentName;
    private final String sender;
    private final String text;
    private final String sentAt;

    // Added for HW7 Part C
    private boolean read;

    // Keep the original constructor for existing code.
    public Message(int id, String agentName, String sender,
                   String text, String sentAt) {
        this(id, agentName, sender, text, sentAt, false);
    }

    // New constructor that includes read/unread status.
    public Message(int id, String agentName, String sender,
                   String text, String sentAt, boolean read) {
        this.id = id;
        this.agentName = agentName;
        this.sender = sender;
        this.text = text;
        this.sentAt = sentAt;
        this.read = read;
    }

    public int getId() {
        return id;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getSender() {
        return sender;
    }

    public String getText() {
        return text;
    }

    public String getSentAt() {
        return sentAt;
    }

    // Returns true if the message has been read.
    public boolean isRead() {
        return read;
    }

    // Marks the message as read in memory.
    public void markAsRead() {
        this.read = true;
    }

    // One readable line for the terminal.
    @Override
    public String toString() {
        return sentAt + "  from " + sender + ": " + text;
    }
}
