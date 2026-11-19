package model;

import static org.junit.jupiter.api.Assertions.*;
import model.Facility;
import org.junit.jupiter.api.Test;

class FacilityCountryTest {

    @Test
    void oldConstructorMeansNoCountry() {
        Facility f = new Facility(1, "Langley", "LNG");
        assertEquals(0, f.getCountryId());
    }

    @Test
    void oldConstructorStillSetsTheOtherFields() {
        Facility f = new Facility(1, "Langley", "LNG");
        assertEquals(1, f.getId());
        assertEquals("Langley", f.getName());
        assertEquals("LNG", f.getAbbreviation());
    }

    @Test
    void newConstructorStoresCountryId() {
        Facility f = new Facility(2, "Oslo Station", "OSL", 5);
        assertEquals(2, f.getId());
        assertEquals("Oslo Station", f.getName());
        assertEquals("OSL", f.getAbbreviation());
        assertEquals(5, f.getCountryId());
    }

    @Test
    void zeroCountryIdIsAllowedAsNoCountry() {
        assertEquals(0, new Facility(3, "Nowhere", "NWH", 0).getCountryId());
    }

    @Test
    void negativeCountryIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Facility(3, "Nowhere", "NWH", -1));
    }
}