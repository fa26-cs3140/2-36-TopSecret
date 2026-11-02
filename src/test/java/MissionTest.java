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

//    @Test
//    void homework6ConstructorKeepsBriefsAndAllowsNoAgents() {
//        Mission empty = new Mission(1, "Op", "b", "d", 0, List.of(), List.of());
//        assertEquals(0, empty.getFacilityId());
//        assertTrue(empty.getAgentIds().isEmpty());
//        assertTrue(empty.getBriefIds().isEmpty());
//
//        Mission m = new Mission(2, "Op", "b", "d", 3, List.of(4, 5, 4), List.of(8, 9, 8));
//        assertEquals(3, m.getFacilityId());
//        assertEquals(List.of(4, 5), m.getAgentIds());
//        assertEquals(List.of(8, 9), m.getBriefIds());
//    }
//
//    @Test
//    void homework6ConstructorRejectsBadInput() {
//        assertThrows(IllegalArgumentException.class, () -> new Mission(1, "Op", "b", "d", -1, List.of(), List.of()));
//        assertThrows(IllegalArgumentException.class, () -> new Mission(1, "Op", "b", "d", 1, null, List.of()));
//        assertThrows(IllegalArgumentException.class,
//                () -> new Mission(1, "Op", "b", "d", 1, java.util.Arrays.asList(1, null), List.of()));
//        assertThrows(IllegalArgumentException.class, () -> new Mission(1, "Op", "b", "d", 1, List.of(), null));
//        assertThrows(IllegalArgumentException.class,
//                () -> new Mission(1, "Op", "b", "d", 1, List.of(), java.util.Arrays.asList(2, null)));
//    }
//
//    @Test
//    void gettersReturnCopiesOfAgentAndBriefIds() {
//        Mission m = new Mission(1, "Op", "b", "d", 1, List.of(3), List.of(4));
//        m.getAgentIds().add(99);
//        m.getBriefIds().add(99);
//        assertEquals(List.of(3), m.getAgentIds());
//        assertEquals(List.of(4), m.getBriefIds());
//    }
//
//    @Test
//    void hasAgentChecksAssignment() {
//        Mission m = new Mission(1, "Op", "b", "d", 1, List.of(3), List.of());
//        assertTrue(m.hasAgent(3));
//        assertFalse(m.hasAgent(4));
//    }
//
//    @Test
//    void toStringIsOnePlainTextLine() {
//        Mission m = new Mission(4, "Op", "b", "2026-01-01", 2, List.of(1, 2), List.of(8));
//        assertEquals("Mission #4: Op (2026-01-01), facility 2, 2 agent(s), 1 brief(s)", m.toString());
//        assertTrue(new Mission(5, "Old", "b", "d").toString().contains("no facility"));
//    }
}
