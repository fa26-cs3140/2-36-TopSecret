import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuditEntryTest {

    private final AuditEntry entry =
            new AuditEntry(7, "2026-10-22T09:30:00", "smayan", "READ", "Brief #3: Project Bluebird");

    @Test
    void constructorAndGettersReturnValues() {
        assertEquals(7, entry.getId());
        assertEquals("2026-10-22T09:30:00", entry.getTimestamp());
        assertEquals("smayan", entry.getUsername());
        assertEquals("READ", entry.getAction());
        assertEquals("Brief #3: Project Bluebird", entry.getItem());
    }

    @Test
    void toStringShowsWhenWhoWhatAndItem() {
        assertEquals("2026-10-22T09:30:00  smayan  READ  Brief #3: Project Bluebird", entry.toString());
    }
}
