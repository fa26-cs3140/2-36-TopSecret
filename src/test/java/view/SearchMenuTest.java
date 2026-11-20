package view;

import model.AuditEntry;
import model.AuditLog;
import model.ModelDatabase;
import model.RecordSearch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

// Terminal search across all records (Member D), with a real in-memory database
class SearchMenuTest {

    private Connection connection;
    private RecordSearch search;
    private final List<String> audit = new ArrayList<>();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        ModelDatabase db = new ModelDatabase(connection);
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE missions (id INTEGER PRIMARY KEY, title TEXT, "
                    + "brief TEXT, date TEXT)");
        }
        db.createTables();
        int harbor = db.addFacility("Harbor Outpost", "HBO");
        db.addAgent("Ghost", null, null, "Works the harbor", harbor);
        db.addCountry("Norway");
        search = new RecordSearch(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    private void run(String input) {
        new SearchMenu(search, new AuditLog() {
            @Override
            public void record(String username, String action, String item) {
                audit.add(username + " " + action + " " + item);
            }

            @Override
            public List<AuditEntry> getEntries() {
                return List.of();
            }
        }, "smayan", new Scanner(input), new PrintStream(output, true)).run();
    }

    @Test
    void listsEveryMatchAndAuditsTheSearch() {
        run("harbor\n");

        String text = output.toString();
        assertTrue(text.contains("2 matches:"), text);
        assertTrue(text.contains("Agent #1: Ghost"), text);
        assertTrue(text.contains("Facility #1: Harbor Outpost"), text);
        assertEquals(List.of("smayan READ Search: harbor"), audit);
    }

    @Test
    void singleMatchUsesSingularWord() {
        run("norway\n");

        assertTrue(output.toString().contains("1 match:"));
        assertTrue(output.toString().contains("Country #1: Norway"));
    }

    @Test
    void saysWhenNothingMatches() {
        run("zzz\n");

        assertTrue(output.toString().contains("No matches."));
        assertEquals(1, audit.size());
    }

    @Test
    void blankTermOrNoInputSearchesNothing() {
        run("   \n");
        run("");

        assertTrue(output.toString().contains("Nothing to search for."));
        assertTrue(audit.isEmpty());
    }

    @Test
    void databaseErrorIsReportedNotThrown() throws Exception {
        connection.close();

        run("harbor\n");

        assertTrue(output.toString().contains("Database error"));
        assertTrue(audit.isEmpty());
    }
}
