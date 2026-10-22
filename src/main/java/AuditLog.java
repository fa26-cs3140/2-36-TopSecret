import java.util.List;

// The audit log (Member D). Every read or create of content is recorded
// with who did it and when. The UI calls record(...) and getEntries().
public interface AuditLog {

    String READ = "READ";
    String CREATE = "CREATE";

    // Saves one event, for example record("smayan", AuditLog.READ, "Brief #3: Project Bluebird")
    void record(String username, String action, String item);

    // All events, oldest first
    List<AuditEntry> getEntries();
}
