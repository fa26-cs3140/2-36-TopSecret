import org.junit.jupiter.api.Test;

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
}