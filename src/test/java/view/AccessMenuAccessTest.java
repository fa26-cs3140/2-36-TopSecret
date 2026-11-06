package view;

import model.Access;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Integration test: real AccessMenu with real Access and database
class AccessMenuAccessTest {

    @TempDir
    Path dir;

    private Access access;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        access = new Access(dir.resolve("test.db"));
        output = new ByteArrayOutputStream();
        access.registerUser("alice"); // ADMIN
        access.registerUser("bobby"); // STANDARD
    }

    private void run(String user, String typed) {
        new AccessMenu(access, new Scanner(typed), new PrintStream(output)).run(user);
    }

    @Test
    void adminPromotesThenDemotes() {
        run("alice", "2\nbobby\n4\n");
        assertTrue(access.isAdmin("bobby"));

        run("alice", "3\nbobby\n4\n");
        assertFalse(access.isAdmin("bobby"));
    }

    @Test
    void adminCannotDemoteThemselves() {
        run("alice", "3\nalice\n4\n");
        assertTrue(output.toString().contains("You cannot demote yourself"));
        assertTrue(access.isAdmin("alice"));
    }

    @Test
    void standardUserCannotUseTheMenu() {
        run("bobby", "2\nbobby\n4\n");
        assertTrue(output.toString().contains("Access denied."));
        assertFalse(access.isAdmin("bobby"));
    }

    @Test
    void listShowsRealLevels() {
        run("alice", "1\n4\n");
        assertTrue(output.toString().contains("alice - ADMIN"));
        assertTrue(output.toString().contains("bobby - STANDARD"));
    }
}