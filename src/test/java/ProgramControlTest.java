import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProgramControlTest {

    @Test
    void programControlCanBeCreated() {
        ProgramControl control = new ProgramControl();

        assertNotNull(control);
    }

    @Test
    void missingFileReturnsNull() {
        ProgramControl control = new ProgramControl();

        String result = control.getFileContents("doesNotExist.txt");

        assertNull(result);
    }

    @Test
    void availableFilesCanBeRequested() {
        ProgramControl control = new ProgramControl();

        List<String> files = control.getAvailableFiles();

        assertNotNull(files);
    }

    @Test
    void missingCipheredFileReturnsNull() {
        ProgramControl control = new ProgramControl();

        String result = control.decipherFile("doesNotExist.cip");

        assertNull(result);
    }

    @Test
    void alternateKeyCanBeUsed() {
        ProgramControl control = new ProgramControl();

        String result = control.decipherFile("carnivore.cip", "key.txt");

        assertTrue(result.startsWith("Carnivore"));
    }
}