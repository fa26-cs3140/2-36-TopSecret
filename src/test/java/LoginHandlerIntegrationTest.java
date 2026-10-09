import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

public class LoginHandlerIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void firstRunCreatesAccountThenLoginWorks()
            throws IOException {

        CredentialManager manager = new CredentialManager(
                tempDir.resolve("credentials.cip"),
                Path.of("ciphers", "key.txt")
        );

        Scanner setupInput =
                new Scanner("agent\nsecret123\n");

        LoginHandler setup =
                new LoginHandler(manager, setupInput);

        // First run must create an account and stop.
        assertFalse(setup.authenticate());
        assertTrue(manager.credentialsExist());

        // Second run accepts the saved credentials.
        Scanner loginInput =
                new Scanner("agent\nsecret123\n");

        LoginHandler login =
                new LoginHandler(manager, loginInput);

        assertTrue(login.authenticate());
    }

    @Test
    void incorrectLoginBlocksAccess() throws IOException {

        CredentialManager manager = new CredentialManager(
                tempDir.resolve("credentials.cip"),
                Path.of("ciphers", "key.txt")
        );

        manager.createCredentials("agent", "secret123");

        LoginHandler login = new LoginHandler(
                manager,
                new Scanner("agent\nwrongpassword\n")
        );

        assertFalse(login.authenticate());
    }
}