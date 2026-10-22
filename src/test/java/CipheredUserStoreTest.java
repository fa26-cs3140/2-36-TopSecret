import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CipheredUserStoreTest {

    @TempDir
    Path dir;

    private CipheredUserStore store() {
        return new CipheredUserStore(dir.resolve("users.cip"), Path.of("ciphers", "key.txt"));
    }

    @Test
    void defaultConstructorUsesDefaultFiles() {
        assertNotNull(new CipheredUserStore());
        assertEquals(Path.of("users.cip"), CipheredUserStore.DEFAULT_USERS_FILE);
        assertEquals(Path.of("ciphers", "key.txt"), CipheredUserStore.DEFAULT_KEY_FILE);
    }

    @Test
    void missingFileMeansNoAccounts() {
        assertTrue(store().loadAll().isEmpty());
    }

    @Test
    void savedAccountsLoadBack() {
        store().saveAll(List.of(new UserAccount("agent", "Secret1", true),
                                new UserAccount("bob", null, false)));
        List<UserAccount> loaded = store().loadAll();
        assertEquals(2, loaded.size());
        assertEquals("agent", loaded.get(0).getUsername());
        assertEquals("Secret1", loaded.get(0).getPassword());
        assertTrue(loaded.get(0).isAdmin());
        assertEquals("bob", loaded.get(1).getUsername());
        assertFalse(loaded.get(1).hasPassword());
        assertFalse(loaded.get(1).isAdmin());
    }

    @Test
    void fileIsCipheredNotPlainText() throws Exception {
        store().saveAll(List.of(new UserAccount("agent", "Secret1", true)));
        String raw = Files.readString(dir.resolve("users.cip"));
        assertFalse(raw.contains("agent"));
        assertFalse(raw.contains("Secret1"));
        assertTrue(raw.contains("bhfou"));
    }

    @Test
    void missingKeyFileUsesBuiltInKey() throws Exception {
        CipheredUserStore noKey = new CipheredUserStore(dir.resolve("users.cip"), dir.resolve("none.txt"));
        noKey.saveAll(List.of(new UserAccount("agent", "Secret1", true)));
        assertTrue(Files.readString(dir.resolve("users.cip")).contains("bhfou"));
        assertEquals("agent", noKey.loadAll().get(0).getUsername());
    }

    @Test
    void savingCreatesMissingFolders() {
        CipheredUserStore nested = new CipheredUserStore(dir.resolve("a/b/users.cip"), Path.of("ciphers", "key.txt"));
        nested.saveAll(List.of(new UserAccount("agent", "Secret1", true)));
        assertTrue(Files.exists(dir.resolve("a/b/users.cip")));
    }

    @Test
    void badLinesAreSkipped() throws Exception {
        Cipher cipher = new Cipher(Files.readString(Path.of("ciphers", "key.txt")));
        Files.writeString(dir.resolve("users.cip"),
                cipher.encipher("agent\tSecret1\tADMIN\r\nbroken line\n\tnoName\tUSER\n"));
        List<UserAccount> loaded = store().loadAll();
        assertEquals(1, loaded.size());
        assertEquals("agent", loaded.get(0).getUsername());
    }

    @Test
    void unreadableFilesThrowUncheckedIOException() throws Exception {
        Files.createDirectory(dir.resolve("users.cip"));
        assertThrows(UncheckedIOException.class, () -> store().loadAll());
        assertThrows(UncheckedIOException.class, () -> store().saveAll(List.of()));
        assertThrows(UncheckedIOException.class,
                () -> new CipheredUserStore(dir.resolve("x.cip"), dir.resolve("users.cip")));
    }
}
