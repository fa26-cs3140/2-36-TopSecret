import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileHandler {

    private final Path dataFolder = Path.of("data");
    private final Path cipherFolder = Path.of("ciphers");

    public String readDataFile(String fileName) { // Reads a mission file from the data folder and return its contents
        try {
            return Files.readString(dataFolder.resolve(fileName));
        } catch (IOException e) {
            return null; // Return null if the file can not be found
        }
    }

    public String readCipherFile(String fileName) {    // Reads a cipher key from the ciphers folder
        try {
            return Files.readString(cipherFolder.resolve(fileName));
        } catch (IOException e) {
            return null; // Return null if the file can not be found
        }
    }

    public List<String> getAvailableFiles() {   // Returns a list of available mission files in the data folder

        List<String> files = new ArrayList<>(); // Stores the available file names

        try {
            Files.list(dataFolder)
                    .filter(Files::isRegularFile)  // Only includes regular files
                    .forEach(file -> // Adds each file name to the list
                            files.add(file.getFileName().toString()));

        } catch (IOException e) {
            return files; // Returns current list
        }

        Collections.sort(files); //Sorts the files names

        return files;
    }
}