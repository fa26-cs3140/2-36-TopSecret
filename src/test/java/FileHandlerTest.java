import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FileHandlerTest {
    @Test
    void missingFileReturnsNull() {
        FileHandler handler = new FileHandler();

        String result = handler.readFile("doesNotExist.txt");

        assertNull(result);
    }

    @Test
    void existingFileReturnsContents() {
        FileHandler handler = new FileHandler();

        String result = handler.readFile("filea.txt");

        assertNotNull(result);
    }
}
