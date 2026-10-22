import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CipherTest {

    private static final String KEY =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890\n"
                    + "bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890a";

    @Test
    void decipherWord() {
        assertEquals("Carnivore", new Cipher(KEY).decipher("Dbsojwpsf"));
    }

    @Test
    void decipherNumbers() {
        assertEquals("1997", new Cipher(KEY).decipher("2008"));
    }

    @Test
    void charactersNotInKeyStayTheSame() {
        assertEquals("(FBI) it's done.", new Cipher(KEY).decipher("(GCJ) ju't epof."));
    }

    @Test
    void alternateKeyWorks() {
        assertEquals("cab", new Cipher("abc\nxyz").decipher("zxy"));
    }

    @Test
    void emptyKeyLeavesTextUnchanged() {
        assertEquals("Hello", new Cipher("").decipher("Hello"));
    }

    @Test
    void keyWithOneLineIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Cipher("abc"));
    }

    @Test
    void keyWithDifferentLengthLinesIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Cipher("abcd\nbca"));
    }

    @Test
    void encipherWord() {
        assertEquals("Dbsojwpsf", new Cipher(KEY).encipher("Carnivore"));
    }

    @Test
    void encipherThenDecipherGivesOriginalText() {
        Cipher c = new Cipher(KEY);
        String text = "agent\tSecret123\tADMIN";
        assertEquals(text, c.decipher(c.encipher(text)));
    }
}
