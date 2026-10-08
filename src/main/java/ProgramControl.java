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
        return decipherFile(fileName, "key.txt");
    }

    // Get and decipher a file using a key file from the ciphers folder
    public String decipherFile(String fileName, String keyFileName) {
        String contents = fileHandler.readDataFile(fileName);
        String key = fileHandler.readCipherFile(keyFileName);

        if (contents == null || key == null) {
            return null;
        }

        try {
            Cipher cipher = new Cipher(key);
            return cipher.decipher(contents);
        } catch (IllegalArgumentException e) {
            return null; // bad key: the UI shows an error instead of crashing
        }
    }
}