import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CredentialManager {

    private final Path credentialFile;
    private final Path keyFile;

    public CredentialManager() {
        this(Path.of("credentials.cip"),
                Path.of("ciphers", "key.txt"));
    }

    public CredentialManager(Path credentialFile, Path keyFile) {
        this.credentialFile = credentialFile;
        this.keyFile = keyFile;
    }

    // Usernames must contain lowercase letters only.
    public boolean isValidUsername(String username) {
        return username != null && username.matches("[a-z]+");
    }

    // Passwords must contain at least five characters.
    public boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 5
                && !password.contains("\n")
                && !password.contains("\r");
    }

    // Read and validate the default substitution key.
    private String[] getKeyLines() throws IOException {
        String[] lines = Files.readString(keyFile)
                .strip()
                .split("\\R");

        if (lines.length != 2
                || lines[0].isEmpty()
                || lines[0].length() != lines[1].length()) {
            throw new IOException("Invalid cipher key");
        }

        for (int i = 0; i < lines[0].length(); i++) {
            if (lines[0].indexOf(lines[0].charAt(i)) != i
                    || lines[1].indexOf(lines[1].charAt(i)) != i) {
                throw new IOException("Duplicate characters in cipher key");
            }
        }

        return lines;
    }

    // Encrypt credentials with the default cipher.
    private String encrypt(String text) throws IOException {
        String[] lines = getKeyLines();
        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            int index = lines[0].indexOf(c);

            if (index >= 0) {
                result.append(lines[1].charAt(index));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    // Decrypt credentials using the existing Cipher class.
    private String decryptCredentials() throws IOException {
        String encrypted = Files.readString(credentialFile);
        String key = Files.readString(keyFile);

        getKeyLines();

        Cipher cipher = new Cipher(key);
        return cipher.decipher(encrypted);
    }

    // Check whether a credential file exists.
    public boolean credentialsExist() {
        return Files.exists(credentialFile);
    }

    // Create credentials only if no account exists.
    public void createCredentials(String username, String password)
            throws IOException {

        if (!isValidUsername(username)
                || !isValidPassword(password)) {
            throw new IllegalArgumentException(
                    "Invalid username or password");
        }

        String credentials = username + "\n" + password;
        String encrypted = encrypt(credentials);

        Files.writeString(
                credentialFile,
                encrypted,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    // Verify that the saved credential file is valid.
    public boolean hasValidCredentialFile() throws IOException {
        if (!Files.isRegularFile(credentialFile)) {
            return false;
        }

        try {
            String stored = decryptCredentials();
            String[] parts = stored.split("\\R", -1);

            return parts.length == 2
                    && isValidUsername(parts[0])
                    && isValidPassword(parts[1])
                    && encrypt(stored).equals(
                    Files.readString(credentialFile));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Verify the entered username and password.
    public boolean login(String username, String password)
            throws IOException {

        if (!hasValidCredentialFile()) {
            return false;
        }

        String[] parts = decryptCredentials().split("\\R", -1);

        return parts[0].equals(username)
                && parts[1].equals(password);
    }

    // Change password after verifying existing credentials.
    public boolean changePassword(
            String username,
            String oldPassword,
            String newPassword,
            String confirmPassword) throws IOException {

        if (!login(username, oldPassword)) {
            return false;
        }

        if (!isValidPassword(newPassword)
                || !newPassword.equals(confirmPassword)) {
            return false;
        }

        String credentials = username + "\n" + newPassword;

        Files.writeString(
                credentialFile,
                encrypt(credentials),
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        return true;
    }
}