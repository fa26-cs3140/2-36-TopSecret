import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentTest {

    @Test
    void constructorAndGetters() {
        Agent a = new Agent(3, "Ghost", "1992-11-22", "2020-01-01", "notes", 2);
        assertEquals(3, a.getId());
        assertEquals("Ghost", a.getName());
        assertEquals("1992-11-22", a.getDateOfBirth());
        assertEquals("2020-01-01", a.getDateOfDeath());
        assertEquals("notes", a.getNotes());
        assertEquals(2, a.getFacilityId());
    }

    @Test
    void settersChangeValues() {
        Agent a = new Agent(1, "Ghost", null, null, null, 1);
        a.setName("Wraith");
        a.setDateOfBirth("1993-12-08");
        a.setDateOfDeath("2025-05-05");
        a.setNotes("retired");
        a.setFacilityId(4);
        assertEquals("Wraith", a.getName());
        assertEquals("1993-12-08", a.getDateOfBirth());
        assertEquals("2025-05-05", a.getDateOfDeath());
        assertEquals("retired", a.getNotes());
        assertEquals(4, a.getFacilityId());
    }
}
