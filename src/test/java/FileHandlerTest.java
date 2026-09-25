import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
public class FileHandlerTest {

    @Test
    void readDataFileReturnsContents() { // Tests that an existing data file can be read
        FileHandler handler = new FileHandler();

        String result = handler.readDataFile("carnivore.cip");

        assertNotNull(result);
    }

    @Test
    void missingDataFileReturnsNull() {  // Tests what happens when a data file does not exist
        FileHandler handler = new FileHandler();

        String result = handler.readDataFile("doesNotExist.txt");

        assertNull(result);
    }

    @Test
    void readCipherFileReturnsContents() { // Tests that an existing cipher key file can be read
        FileHandler handler = new FileHandler();

        String result = handler.readCipherFile("key.txt");

        assertNotNull(result);
    }

    @Test
    void missingCipherFileReturnsNull() { // Tests what happens when a cipher file does not exist
        FileHandler handler = new FileHandler();

        String result = handler.readCipherFile("doesNotExist.txt");

        assertNull(result);
    }

    @Test
    void getAvailableFilesReturnsList() {     // Tests that the available files can be retrieved
        FileHandler handler = new FileHandler();

        List<String> files = handler.getAvailableFiles();

        assertNotNull(files);
    }
}