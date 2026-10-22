import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Saves user accounts in a ciphered file (Member D).
// Each line before ciphering is: username <tab> password <tab> ADMIN or USER
// An empty password means the user has not set one yet.
public class CipheredUserStore implements UserStore {

    public static final Path DEFAULT_USERS_FILE = Path.of("users.cip");
    public static final Path DEFAULT_KEY_FILE = Path.of("ciphers", "key.txt");

    // Used when ciphers/key.txt is missing, so the program still runs
    static final String BUILT_IN_KEY =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890\n"
          + "bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890a";

    private final Path usersFile;
    private final Cipher cipher;

    public CipheredUserStore() {
        this(DEFAULT_USERS_FILE, DEFAULT_KEY_FILE);
    }

    public CipheredUserStore(Path usersFile, Path keyFile) {
        this.usersFile = usersFile;
        this.cipher = new Cipher(readKey(keyFile));
    }

    private static String readKey(Path keyFile) {
        try {
            return Files.exists(keyFile) ? Files.readString(keyFile) : BUILT_IN_KEY;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read cipher key", e);
        }
    }

    @Override
    public List<UserAccount> loadAll() {
        List<UserAccount> users = new ArrayList<>();
        if (!Files.exists(usersFile)) {
            return users;
        }
        try {
            String text = cipher.decipher(Files.readString(usersFile));
            for (String line : text.split("\\R")) {
                String[] parts = line.split("\t", -1);
                if (parts.length == 3 && !parts[0].isEmpty()) {
                    String password = parts[1].isEmpty() ? null : parts[1];
                    users.add(new UserAccount(parts[0], password, parts[2].equals("ADMIN")));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read user accounts", e);
        }
        return users;
    }

    @Override
    public void saveAll(List<UserAccount> users) {
        StringBuilder text = new StringBuilder();
        for (UserAccount u : users) {
            text.append(u.getUsername()).append('\t')
                .append(u.hasPassword() ? u.getPassword() : "").append('\t')
                .append(u.isAdmin() ? "ADMIN" : "USER").append('\n');
        }
        try {
            Path parent = usersFile.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Files.writeString(usersFile, cipher.encipher(text.toString()));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save user accounts", e);
        }
    }
}
