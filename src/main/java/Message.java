// Stores message ID, sender, recipient, content, and read status.

public class Message {
    private final int id;
    private final String sender;
    private final String recipient;
    private final String content;
    private boolean read;

    public Message(int id, String sender,
                   String recipient, String content,
                   boolean read) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.read = read;
    }

    public int getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    public boolean isRead() {
        return read;
    }

    public void markAsRead() {
        this.read = true;
    }
}
