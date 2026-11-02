import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MissionMenuTest {

    // Fake database so these tests don't need SQLite
    private final List<Mission> data = List.of(
            new Mission(1, "Operation Sandtrap", "Bug the diplomatic pouch.", "1970-11-03"),
            new Mission(2, "The Munich Lead", "Identify the cell in the safehouse.", "1972-09-15"));

    private final MissionRepository fakeRepo = () -> data;

    private final MissionSearch fakeSearch = query -> data.stream()
            .filter(m -> m.getBrief().toLowerCase().contains(query.toLowerCase()))
            .toList();

    // Runs the menu with the given typed input and returns everything it printed
    private String runWith(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MissionMenu menu = new MissionMenu(fakeRepo, fakeSearch,
                new Scanner(input), new PrintStream(output));
        menu.run();
        return output.toString();
    }

    @Test
    void exitOptionEndsProgram() {
        assertTrue(runWith("4\n").contains("Goodbye."));
    }

    @Test
    void listShowsNumberedTitles() {
        String out = runWith("1\n4\n");
        assertTrue(out.contains("1. Operation Sandtrap"));
        assertTrue(out.contains("2. The Munich Lead"));
    }

    @Test
    void readShowsMissionBrief() {
        String out = runWith("2\n2\n4\n");
        assertTrue(out.contains("Identify the cell in the safehouse."));
    }

    @Test
    void menuShowsAgainAfterReading() {
        String out = runWith("2\n1\n4\n");
        int first = out.indexOf("1. List missions");
        assertTrue(out.indexOf("1. List missions", first + 1) > first);
    }

    @Test
    void readWithBadNumberShowsError() {
        assertTrue(runWith("2\n99\n4\n").contains("No mission with number 99"));
    }

    @Test
    void readWithNonNumberShowsError() {
        assertTrue(runWith("2\nabc\n4\n").contains("is not a number"));
    }

    @Test
    void invalidMenuChoiceShowsError() {
        assertTrue(runWith("7\n4\n").contains("Invalid choice"));
    }

    @Test
    void searchShowsMatches() {
        assertTrue(runWith("3\nSAFEHOUSE\n4\n").contains("- The Munich Lead"));
    }

    @Test
    void searchWithNoMatchesSaysSo() {
        assertTrue(runWith("3\nzebra\n4\n").contains("No matches found."));
    }

    @Test
    void emptyDatabaseShowsMessage() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new MissionMenu(List::of, fakeSearch, new Scanner("1\n4\n"), new PrintStream(output)).run();
        assertTrue(output.toString().contains("No missions available."));
    }

    // ---------- Homework 5: audit log and adding users (Mockito mocks) ----------

    private final AuditLog audit = mock(AuditLog.class);
    private final UserStore users = mock(UserStore.class);
    private final UserAccount agent = new UserAccount("agent", "Secret1", true);

    private String runLoggedIn(String input) {
        when(users.loadAll()).thenReturn(new ArrayList<>(List.of(agent)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new MissionMenu(fakeRepo, fakeSearch, new Scanner(input), new PrintStream(output),
                audit, new AccountManager(users), agent).run();
        return output.toString();
    }

    @Test
    void readingAMissionIsWrittenToAuditLog() {
        runLoggedIn("2\n2\n4\n");
        verify(audit).record("agent", AuditLog.READ, "Mission brief #2: The Munich Lead");
    }

    @Test
    void listingAndSearchingAreNotLoggedAsReads() {
        runLoggedIn("1\n3\nsafehouse\n4\n");
        verify(audit, never()).record(anyString(), anyString(), anyString());
    }

    @Test
    void addUserOptionShownWhenLoggedIn() {
        assertTrue(runLoggedIn("4\n").contains("5. Add user"));
        assertFalse(runWith("4\n").contains("5. Add user"));
    }

    @Test
    void addUserSavesAccountAndLogsCreate() {
        String out = runLoggedIn("5\nbob_2\n4\n");
        assertTrue(out.contains("User 'bob_2' added."));
        verify(users).saveAll(anyList());
        verify(audit).record("agent", AuditLog.CREATE, "User account: bob_2");
    }

    @Test
    void addUserWithBadNameShowsErrorAndLogsNothing() {
        String out = runLoggedIn("5\nagent\n4\n");
        assertTrue(out.contains("Error: Username already exists"));
        verify(users, never()).saveAll(anyList());
        verify(audit, never()).record(anyString(), anyString(), anyString());
    }

    @Test
    void addUserWithNoInputReturnsToMenu() {
        runLoggedIn("5\n");
        verify(users, never()).saveAll(anyList());
    }

    @Test
    void optionFiveIsInvalidWithoutAccounts() {
        assertTrue(runWith("5\n4\n").contains("Invalid choice"));
    }

    @Test
    void standardUserDoesNotSeeOrUseAddUser() {
        UserAccount bob = new UserAccount("bob", "BobPass", false);
        when(users.loadAll()).thenReturn(new ArrayList<>(List.of(agent, bob)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new MissionMenu(fakeRepo, fakeSearch, new Scanner("5\n4\n"), new PrintStream(output),
                audit, new AccountManager(users), bob).run();
        assertFalse(output.toString().contains("5. Add user"));
        assertTrue(output.toString().contains("Invalid choice"));
        verify(users, never()).saveAll(anyList());
    }
}
