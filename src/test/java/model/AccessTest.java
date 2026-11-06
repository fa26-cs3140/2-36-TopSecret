package model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccessTest {

    @TempDir
    Path dir;

    private Access access;

    @BeforeEach
    void setUp() {
        access = new Access(dir.resolve("test.db"));
        access.registerUser("alice"); // first user, so ADMIN
        access.registerUser("bobby"); // STANDARD
    }

    @Test
    void defaultConstructorWorks() {
        assertNotNull(new Access());
    }

    @Test
    void firstUserIsAdminAndLaterUsersAreStandard() {
        assertEquals("ADMIN", access.getLevel("alice"));
        assertEquals("STANDARD", access.getLevel("bobby"));
    }

    @Test
    void registeringTwiceChangesNothing() {
        access.registerUser("alice");
        access.registerUser("bobby");
        assertEquals(2, access.getAllUsers().size());
        assertEquals("ADMIN", access.getLevel("alice"));
    }

    @Test
    void registerNeedsAUsername() {
        assertThrows(IllegalArgumentException.class, () -> access.registerUser(null));
        assertThrows(IllegalArgumentException.class, () -> access.registerUser(" "));
    }

    @Test
    void unknownUserHasNoLevel() {
        assertNull(access.getLevel("nobody"));
    }

    @Test
    void isAdminOnlyForAdmins() {
        assertTrue(access.isAdmin("alice"));
        assertFalse(access.isAdmin("bobby"));
        assertFalse(access.isAdmin("nobody"));
    }

    @Test
    void adminCanPromote() {
        access.promote("alice", "bobby");
        assertTrue(access.isAdmin("bobby"));
    }

    @Test
    void promoteFailsWhenNotAllowed() {
        access.registerUser("carol");
        assertThrows(IllegalArgumentException.class, () -> access.promote("bobby", "carol"));
        assertThrows(IllegalArgumentException.class, () -> access.promote("alice", "nobody"));
        assertThrows(IllegalArgumentException.class, () -> access.promote("alice", "alice"));
    }

    @Test
    void adminCanDemoteAnotherAdmin() {
        access.promote("alice", "bobby");
        access.demote("alice", "bobby");
        assertFalse(access.isAdmin("bobby"));
    }

    @Test
    void demoteFailsWhenNotAllowed() {
        assertThrows(IllegalArgumentException.class, () -> access.demote("bobby", "alice"));
        assertThrows(IllegalArgumentException.class, () -> access.demote("alice", "nobody"));
        assertThrows(IllegalArgumentException.class, () -> access.demote("alice", "bobby"));
    }

    @Test
    void adminCannotDemoteThemselves() {
        assertThrows(IllegalArgumentException.class, () -> access.demote("alice", "alice"));
        assertThrows(IllegalArgumentException.class, () -> access.demote("alice", "ALICE"));
        assertTrue(access.isAdmin("alice"));
    }

    @Test
    void getAllUsersListsEveryoneInOrder() {
        assertEquals("[alice, bobby]", access.getAllUsers().keySet().toString());
    }

    @Test
    void levelsAreSavedInTheDatabase() {
        access.promote("alice", "bobby");
        Access again = new Access(dir.resolve("test.db"));
        assertTrue(again.isAdmin("bobby"));
    }
}