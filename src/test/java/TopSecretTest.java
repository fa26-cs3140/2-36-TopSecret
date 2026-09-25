import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TopSecretTest {
    @Test
    void runsWithNoArgumentsWithoutCrashing() {
        assertDoesNotThrow(() -> TopSecret.main(new String[]{}));
    }
}