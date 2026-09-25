import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
public class FileHandlerTest {

    @Test
    void readDataFileReturnsContents() {
        FileHandler handler = new FileHandler();

        String result = handler.readDataFile("filea.txt");

        assertNotNull(result);
    }

    @Test
    void missingDataFileReturnsNull() {
        FileHandler handler = new FileHandler();

        String result = handler.readDataFile("doesNotExist.txt");

        assertNull(result);
    }

    @Test
    void readCipherFileReturnsContents() {
        FileHandler handler = new FileHandler();

        String result = handler.readCipherFile("key.txt");

        assertNotNull(result);
    }

    @Test
    void missingCipherFileReturnsNull() {
        FileHandler handler = new FileHandler();

        String result = handler.readCipherFile("doesNotExist.txt");

        assertNull(result);
    }

    @Test
    void getAvailableFilesReturnsList() {
        FileHandler handler = new FileHandler();

        List<String> files = handler.getAvailableFiles();

        assertNotNull(files);
    }
}