import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MissionTest {

    @Test
    void homework4ConstructorAndGetters() {
        Mission m = new Mission(1, "Operation Sandtrap", "Bug the pouch.", "1970-11-03");
        assertEquals(1, m.getId());
        assertEquals("Operation Sandtrap", m.getTitle());
        assertEquals("Bug the pouch.", m.getBrief());
        assertEquals("1970-11-03", m.getDate());
        assertEquals(0, m.getFacilityId());
        assertTrue(m.getAgentIds().isEmpty());
    }

    @Test
    void homework5ConstructorKeepsFacilityAndAgents() {
        Mission m = new Mission(2, "Op", "brief", "2026-01-01", 3, List.of(4, 5));
        assertEquals(3, m.getFacilityId());
        assertEquals(List.of(4, 5), m.getAgentIds());
    }

    @Test
    void missionNeedsAFacilityAndAtLeastOneAgent() {
        assertThrows(IllegalArgumentException.class, () -> new Mission(1, "Op", "b", "d", 0, List.of(1)));
        assertThrows(IllegalArgumentException.class, () -> new Mission(1, "Op", "b", "d", 1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Mission(1, "Op", "b", "d", 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Mission(1, "Op", "b", "d", 1, java.util.Arrays.asList(1, null)));
    }

    @Test
    void equalsAndHashCodeCompareFields() {
        Mission a = new Mission(1, "Op", "b", "d");
        Mission b = new Mission(1, "Op", "b", "d");
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new Mission(2, "Op", "b", "d"));
        assertNotEquals(a, "not a mission");
    }
}
