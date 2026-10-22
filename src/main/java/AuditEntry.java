// One row of the audit log (Member D): when, who, what action, on what item.
public class AuditEntry {

    private final int id;
    private final String timestamp;
    private final String username;
    private final String action;
    private final String item;

    public AuditEntry(int id, String timestamp, String username, String action, String item) {
        this.id = id;
        this.timestamp = timestamp;
        this.username = username;
        this.action = action;
        this.item = item;
    }

    public int getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getItem() {
        return item;
    }

    // One readable line for the audit log screen
    @Override
    public String toString() {
        return timestamp + "  " + username + "  " + action + "  " + item;
    }
}
