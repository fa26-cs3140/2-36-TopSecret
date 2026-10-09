import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

// Integration tests: the menu (Member D) running on the real SQLite database (Member A)
// loaded from the real mission_briefs.tsv file.
class MissionMenuIntegrationTest {

    @TempDir
    Path tempDir;

    // Runs the menu against a real database with the given typed input
    private String runWith(String input) {
        MissionStore store = new SqliteMissionStore(tempDir.resolve("test.db"));
        store.importFromTsv(SqliteMissionStore.DEFAULT_TSV_FILE);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new MissionMenu(store, store, new Scanner(input), new PrintStream(output)).run();
        return output.toString();
    }

    @Test
    void listShowsMissionsFromDatabase() {
        String out = runWith("1\n4\n");
        assertTrue(out.contains("1. Operation Sandtrap"));
        assertTrue(out.contains("100. The Quantum Leap"));
    }

    @Test
    void readShowsBriefFromDatabase() {
        String out = runWith("2\n1\n4\n");
        assertTrue(out.contains("Operation Sandtrap (1970-11-03)"));
        assertTrue(out.contains("Bug the diplomatic pouch of the Libyan attach"));
    }

    @Test
    void searchFindsMatchesInDatabaseIgnoringCase() {
        String out = runWith("3\nKREMLIN\n4\n");
        assertTrue(out.contains("- Operation Able Archer"));
    }

    @Test
    void searchWithNoMatchesInDatabaseSaysSo() {
        assertTrue(runWith("3\nxyzzy\n4\n").contains("No matches found."));
    }
}
