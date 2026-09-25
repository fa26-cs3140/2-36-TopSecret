import java.util.List;

public class ProgramControl {

    private final FileHandler fileHandler;

    public ProgramControl() {
        fileHandler = new FileHandler();
    }

    // Get the list of mission files from Member C's FileHandler
    public List<String> getAvailableFiles() {
        return fileHandler.getAvailableFiles();
    }

    // Get a file without deciphering it
    public String getFileContents(String fileName) {
        return fileHandler.readDataFile(fileName);
    }

    // Get and decipher a file using the default key
    public String decipherFile(String fileName) {
        String contents = fileHandler.readDataFile(fileName);

        if (contents == null) {
            return null;
        }

        String key = fileHandler.readCipherFile("key.txt");

        if (key == null) {
            return null;
        }

        Cipher cipher = new Cipher(key);
        return cipher.decipher(contents);
    }

    // Get and decipher a file using an alternate key
    public String decipherFile(String fileName, String key) {
        String contents = fileHandler.readDataFile(fileName);

        if (contents == null) {
            return null;
        }

        Cipher cipher = new Cipher(key);
        return cipher.decipher(contents);
    }
}