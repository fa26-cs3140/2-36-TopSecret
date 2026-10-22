import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

// Integration tests: login prompts + AccountManager + real ciphered user file
class LoginHandlerAccountManagerTest {

    @TempDir
    Path dir;

    private UserAccount login(String input) {
        AccountManager accounts = new AccountManager(
                new CipheredUserStore(dir.resolve("users.cip"), Path.of("ciphers", "key.txt")));
        return new LoginHandler(accounts, new Scanner(input), new PrintStream(new ByteArrayOutputStream()))
                .authenticate();
    }

    @Test
    void firstRunThenLoginThenNewUserFirstLogin() {
        assertNull(login("agent\nSecret1\nSecret1\n"));      // first run creates admin, exits
        UserAccount admin = login("agent\nSecret1\n");     // second run logs in
        assertTrue(admin.isAdmin());

        new AccountManager(new CipheredUserStore(dir.resolve("users.cip"), Path.of("ciphers", "key.txt")))
                .addUser(admin, "bob");
        UserAccount bob = login("bob\nBobPass\nBobPass\n"); // bob sets a password
        assertEquals("bob", bob.getUsername());
        assertNotNull(login("bob\nBobPass\n"));             // and can log in with it
        assertNull(login("bob\nwrong\n"));
    }
}
