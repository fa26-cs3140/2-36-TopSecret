import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

// Integration tests: AccountManager with the real ciphered user file
class AccountManagerCipheredUserStoreTest {

    @TempDir
    Path dir;

    // A fresh manager each time, like restarting the program
    private AccountManager restart() {
        return new AccountManager(new CipheredUserStore(dir.resolve("users.cip"), Path.of("ciphers", "key.txt")));
    }

    @Test
    void fullAccountLifecycleSurvivesRestarts() throws Exception {
        UserAccount admin = restart().createFirstAccount("agent", "Secret1");
        restart().addUser(admin, "bob");

        AccountManager afterRestart = restart();
        assertEquals(2, afterRestart.getUsers().size());
        assertTrue(afterRestart.needsPassword("bob"));
        assertTrue(afterRestart.setInitialPassword("bob", "BobPass", "BobPass"));

        assertNotNull(restart().login("bob", "BobPass"));
        assertTrue(restart().changePassword("bob", "BobPass", "BobPass2", "BobPass2"));
        assertNull(restart().login("bob", "BobPass"));
        assertNotNull(restart().login("bob", "BobPass2"));
        assertTrue(restart().findUser("agent").isAdmin());
        assertFalse(restart().findUser("bob").isAdmin());

        String raw = Files.readString(dir.resolve("users.cip"));
        assertFalse(raw.contains("BobPass2"));
    }
}
