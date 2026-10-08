import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

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
}