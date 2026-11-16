//import static org.junit.jupiter.api.Assertions.*;
//
//import org.junit.jupiter.api.Test;
//
//// HW7 Member A: the new Country entity.
//// Contract: public Country(int id, String name); getId(); getName(); equals/hashCode; toString() == name.
//class CountryTest {
//
//    @Test
//    void storesIdAndName() {
//        Country c = new Country(7, "Norway");
//        assertEquals(7, c.getId());
//        assertEquals("Norway", c.getName());
//    }
//
//    @Test
//    void nameIsTrimmed() {
//        assertEquals("Norway", new Country(1, "  Norway  ").getName());
//    }
//
//    @Test
//    void nullNameIsRejected() {
//        assertThrows(IllegalArgumentException.class, () -> new Country(1, null));
//    }
//
//    @Test
//    void blankNameIsRejected() {
//        assertThrows(IllegalArgumentException.class, () -> new Country(1, "   "));
//        assertThrows(IllegalArgumentException.class, () -> new Country(1, ""));
//    }
//
//    @Test
//    void equalWhenIdAndNameMatch() {
//        Country a = new Country(1, "Norway");
//        Country b = new Country(1, "Norway");
//        assertEquals(a, b);
//        assertEquals(a.hashCode(), b.hashCode());
//    }
//
//    @Test
//    void notEqualWhenIdDiffers() {
//        assertNotEquals(new Country(1, "Norway"), new Country(2, "Norway"));
//    }
//
//    @Test
//    void notEqualWhenNameDiffers() {
//        assertNotEquals(new Country(1, "Norway"), new Country(1, "Sweden"));
//    }
//
//    @Test
//    void notEqualToNullOrOtherTypes() {
//        Country c = new Country(1, "Norway");
//        assertNotEquals(null, c);
//        assertNotEquals("Norway", c);
//        assertEquals(c, c);
//    }
//
//    @Test
//    void toStringIsTheName() {
//        assertEquals("Norway", new Country(1, "Norway").toString());
//    }
//}