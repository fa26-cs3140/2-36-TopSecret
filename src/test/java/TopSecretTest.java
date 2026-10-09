
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class TopSecretTest {

    @Test
    void runsWithNoArgumentsWithoutCrashing() {
        InputStream originalIn = System.in;

        try {
            // Simulate no keyboard input
            System.setIn(new ByteArrayInputStream(new byte[0]));

            assertDoesNotThrow(() ->
                    TopSecret.main(new String[]{})
            );

        } finally {
            System.setIn(originalIn);
        }
    }
}