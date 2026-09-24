import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProgramControlTest {

    @Test
    void programControlCanBeCreated() {
        ProgramControl control = new ProgramControl();

        assertNotNull(control);
    }
}