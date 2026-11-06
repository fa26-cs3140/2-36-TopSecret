package view;

import model.Access;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessMenuTest {

    private Access access;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        access = mock(Access.class);
        output = new ByteArrayOutputStream();
        when(access.isAdmin("alice")).thenReturn(true);
    }

    private void run(String user, String typed) {
        new AccessMenu(access, new Scanner(typed), new PrintStream(output)).run(user);
    }

    @Test
    void standardUserIsDenied() {
        run("bobby", "4\n");
        assertTrue(output.toString().contains("Access denied."));
    }

    @Test
    void optionFourExits() {
        run("alice", "4\n");
        assertTrue(output.toString().contains("Choose an option"));
    }

    @Test
    void noInputExits() {
        run("alice", "");
        assertTrue(output.toString().contains("Choose an option"));
    }

    @Test
    void badChoiceShowsError() {
        run("alice", "9\n4\n");
        assertTrue(output.toString().contains("Invalid choice"));
    }

    @Test
    void listShowsUsersAndLevels() {
        Map<String, String> users = new LinkedHashMap<>();
        users.put("alice", "ADMIN");
        users.put("bobby", "STANDARD");
        when(access.getAllUsers()).thenReturn(users);

        run("alice", "1\n4\n");
        assertTrue(output.toString().contains("alice - ADMIN"));
        assertTrue(output.toString().contains("bobby - STANDARD"));
    }

    @Test
    void listWithNoUsers() {
        when(access.getAllUsers()).thenReturn(new LinkedHashMap<>());
        run("alice", "1\n4\n");
        assertTrue(output.toString().contains("No users."));
    }

    @Test
    void promoteCallsAccess() {
        run("alice", "2\nbobby\n4\n");
        verify(access).promote("alice", "bobby");
        assertTrue(output.toString().contains("bobby is now an admin."));
    }

    @Test
    void promoteShowsErrors() {
        doThrow(new IllegalArgumentException("No such user: zed")).when(access).promote("alice", "zed");
        run("alice", "2\nzed\n4\n");
        assertTrue(output.toString().contains("Error: No such user: zed"));
    }

    @Test
    void demoteCallsAccess() {
        run("alice", "3\ncarol\n4\n");
        verify(access).demote("alice", "carol");
        assertTrue(output.toString().contains("carol is now a standard user."));
    }

    @Test
    void demoteShowsErrors() {
        doThrow(new IllegalArgumentException("You cannot demote yourself")).when(access).demote("alice", "alice");
        run("alice", "3\nalice\n4\n");
        assertTrue(output.toString().contains("Error: You cannot demote yourself"));
    }

    @Test
    void missingInputAfterQuestionDoesNotCrash() {
        run("alice", "2\n");
        run("alice", "3\n");
        assertTrue(output.toString().contains("Username to demote:"));
    }
}