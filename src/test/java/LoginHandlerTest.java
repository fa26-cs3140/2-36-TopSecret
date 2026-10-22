import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Unit tests: the user file is a Mockito mock, typed input comes from a Scanner
class LoginHandlerTest {

    private final UserStore store = mock(UserStore.class);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private LoginHandler handler(String input) {
        return new LoginHandler(new AccountManager(store), new Scanner(input), new PrintStream(output));
    }

    private void storeHas(UserAccount... users) {
        when(store.loadAll()).thenReturn(new ArrayList<>(List.of(users)));
    }

    @Test
    void firstRunCreatesAdminAccountThenExits() {
        storeHas();
        assertNull(handler("agent\nSecret1\nSecret1\n").authenticate());
        verify(store).saveAll(anyList());
        assertTrue(output.toString().contains("Account created"));
    }

    @Test
    void firstRunRejectsMismatchedPasswords() {
        storeHas();
        assertNull(handler("agent\nSecret1\nSecret2\n").authenticate());
        verify(store, never()).saveAll(anyList());
        assertTrue(output.toString().contains("Passwords do not match."));
    }

    @Test
    void firstRunRejectsInvalidUsername() {
        storeHas();
        assertNull(handler("ab\nSecret1\nSecret1\n").authenticate());
        assertTrue(output.toString().contains(AccountManager.USERNAME_RULE));
        verify(store, never()).saveAll(anyList());
    }

    @Test
    void firstRunWithNoInputDoesNothing() {
        storeHas();
        assertNull(handler("").authenticate());
        verify(store, never()).saveAll(anyList());
    }

    @Test
    void correctPasswordLogsIn() {
        storeHas(new UserAccount("agent", "Secret1", true));
        UserAccount user = handler("agent\nSecret1\n").authenticate();
        assertEquals("agent", user.getUsername());
        assertTrue(output.toString().contains("Login successful!"));
    }

    @Test
    void wrongPasswordIsRejected() {
        storeHas(new UserAccount("agent", "Secret1", true));
        assertNull(handler("agent\nwrong\n").authenticate());
        assertTrue(output.toString().contains("Incorrect username or password."));
    }

    @Test
    void missingInputDuringLoginReturnsNull() {
        storeHas(new UserAccount("agent", "Secret1", true));
        assertNull(handler("").authenticate());
        assertNull(handler("agent\n").authenticate());
    }

    @Test
    void newUserCreatesPasswordAtFirstLogin() {
        storeHas(new UserAccount("bob", null, false));
        UserAccount user = handler("bob\nBobPass\nBobPass\n").authenticate();
        assertNotNull(user);
        verify(store).saveAll(anyList());
        assertTrue(output.toString().contains("Password set."));
    }

    @Test
    void newUserWithBadPasswordIsNotLoggedIn() {
        storeHas(new UserAccount("bob", null, false));
        assertNull(handler("bob\nbobpass\nbobpass\n").authenticate());
        verify(store, never()).saveAll(anyList());
        assertTrue(output.toString().contains("Password not set."));
    }

    @Test
    void changePasswordSucceedsWithCorrectCurrentPassword() {
        storeHas(new UserAccount("agent", "Secret1", true));
        assertTrue(handler("agent\nSecret1\nBetter2\nBetter2\n").changePassword());
        verify(store).saveAll(anyList());
    }

    @Test
    void changePasswordFailsWithWrongPasswordOrMissingInput() {
        storeHas(new UserAccount("agent", "Secret1", true));
        assertFalse(handler("agent\nwrong\nBetter2\nBetter2\n").changePassword());
        assertFalse(handler("agent\n").changePassword());
        verify(store, never()).saveAll(anyList());
    }
}
