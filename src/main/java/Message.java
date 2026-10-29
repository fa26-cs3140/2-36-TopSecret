// A message left for an agent (Member D).
// Only the user whose username matches the agent's name (ignoring case) can read it.
public class Message {

    private final int id;
    private final String agentName;
    private final String sender;
    private final String text;
    private final String sentAt;

    public Message(int id, String agentName, String sender, String text, String sentAt) {
        this.id = id;
        this.agentName = agentName;
        this.sender = sender;
        this.text = text;
        this.sentAt = sentAt;
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

    // One readable line for the terminal, e.g. "2026-10-29T14:10:00  from smayan: Meet at the safehouse."
    @Override
    public String toString() {
        return sentAt + "  from " + sender + ": " + text;
    }
}
