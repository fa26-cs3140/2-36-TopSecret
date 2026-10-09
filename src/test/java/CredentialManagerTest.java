import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class CredentialManagerTest {

    @TempDir
    Path tempDir;

    private CredentialManager testManager() {
        return new CredentialManager(
                tempDir.resolve("credentials.cip"),
                Path.of("ciphers", "key.txt")
        );
    }

    @Test
    void validUsername() {
        CredentialManager manager = testManager();

        assertTrue(manager.isValidUsername("agent"));
        assertFalse(manager.isValidUsername("Agent"));
        assertFalse(manager.isValidUsername("agent123"));
        assertFalse(manager.isValidUsername(""));
        assertFalse(manager.isValidUsername(null));
    }

    @Test
    void validPassword() {
        CredentialManager manager = testManager();

        assertTrue(manager.isValidPassword("secret"));
        assertTrue(manager.isValidPassword("12345"));
        assertFalse(manager.isValidPassword("1234"));
        assertFalse(manager.isValidPassword(""));
        assertFalse(manager.isValidPassword("secret\n123"));
    }

    @Test
    void credentialsInitiallyMissing() {
        assertFalse(testManager().credentialsExist());
    }

    @Test
    void loginWithoutCredentialsFails() throws IOException {
        assertFalse(testManager().login("agent", "secret123"));
    }

    @Test
    void createsEncryptedCredentials() throws IOException {
        CredentialManager manager = testManager();

        manager.createCredentials("agent", "secret123");

        Path file = tempDir.resolve("credentials.cip");

        assertTrue(Files.exists(file));
        assertFalse(Files.readString(file)
                .equals("agent\nsecret123"));
        assertTrue(manager.hasValidCredentialFile());
    }

    @Test
    void correctLoginWorks() throws IOException {
        CredentialManager manager = testManager();

        manager.createCredentials("agent", "secret123");

        assertTrue(manager.login("agent", "secret123"));
    }

    @Test
    void incorrectLoginFails() throws IOException {
        CredentialManager manager = testManager();

        manager.createCredentials("agent", "secret123");

        assertFalse(manager.login("agent", "wrongpassword"));
        assertFalse(manager.login("wronguser", "secret123"));
    }

    @Test
    void passwordCanBeChanged() throws IOException {
        CredentialManager manager = testManager();

        manager.createCredentials("agent", "secret123");

        assertTrue(manager.changePassword(
                "agent",
                "secret123",
                "newsecret123",
                "newsecret123"
        ));

        assertFalse(manager.login("agent", "secret123"));
        assertTrue(manager.login("agent", "newsecret123"));
    }

    @Test
    void mismatchedPasswordsFail() throws IOException {
        CredentialManager manager = testManager();

        manager.createCredentials("agent", "secret123");

        assertFalse(manager.changePassword(
                "agent",
                "secret123",
                "newsecret123",
                "different123"
        ));

        assertTrue(manager.login("agent", "secret123"));
    }

    @Test
    void existingCredentialsCannotBeOverwritten()
            throws IOException {

        CredentialManager manager = testManager();

        manager.createCredentials("agent", "secret123");

        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> manager.createCredentials(
                        "other", "another123"));
    }

    @Test
    void invalidCredentialFileIsRejected() throws IOException {
        CredentialManager manager = testManager();

        Files.writeString(
                tempDir.resolve("credentials.cip"),
                "invalid\nfile\ncontents"
        );

        assertFalse(manager.hasValidCredentialFile());
        assertFalse(manager.login("agent", "secret123"));
    }

    @Test
    void shortPasswordIsRejected() {
        CredentialManager manager = testManager();

        assertThrows(IllegalArgumentException.class,
                () -> manager.createCredentials("agent", "abc"));
    }
}