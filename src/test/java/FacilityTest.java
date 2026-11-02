import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FacilityTest {

    @Test
    void constructorAndGetters() {
        Facility f = new Facility(2, "Rose Garden", "RG");
        assertEquals(2, f.getId());
        assertEquals("Rose Garden", f.getName());
        assertEquals("RG", f.getAbbreviation());
    }

    @Test
    void settersChangeValues() {
        Facility f = new Facility(1, "Rose Garden", "RG");
        f.setName("Glass House");
        f.setAbbreviation("GA");
        assertEquals("Glass House", f.getName());
        assertEquals("GA", f.getAbbreviation());
    }
}
