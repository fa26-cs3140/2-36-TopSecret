import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHandler {
    private final Path dataFolder = Path.of("data");

    public String readFile(String fileName) {
        try {
            return Files.readString(dataFolder.resolve(fileName));
        } catch (IOException e) {
            return null;
        }
    }
}
