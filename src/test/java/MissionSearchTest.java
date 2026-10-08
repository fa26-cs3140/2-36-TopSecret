import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MissionSearchTest {
    private Connection connection;
    private MissionSearchService searchService;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        Statement stmt = connection.createStatement();
        stmt.execute("CREATE TABLE missions (id INTEGER PRIMARY KEY, title TEXT, brief TEXT, date TEXT)");
        stmt.execute("INSERT INTO missions VALUES (1, 'Operation Alpha', 'Infiltrate the target in Berlin.', '2026-01-10')");
        stmt.execute("INSERT INTO missions VALUES (2, 'Operation Beta', 'Extract top secret documents.', '2026-02-15')");

        searchService = new MissionSearchService(connection);
    }

    @Test
    void searchBriefsReturnsMatchingBrief() {
        List<Mission> matches = searchService.searchBriefs("Berlin");
        assertEquals(1, matches.size());
        assertEquals("Operation Alpha", matches.get(0).getTitle());
    }

    @Test
    void searchBriefsIsCaseInsensitive() {
        // "BERLIN" should match "Berlin"[cite: 2]
        List<Mission> matches = searchService.searchBriefs("BERLIN");
        assertEquals(1, matches.size());
    }

    @Test
    void searchBriefsIgnoresTitle() {
        // Searching for "Beta" (which is in title, not brief) should return 0 results[cite: 2]
        List<Mission> matches = searchService.searchBriefs("Beta");
        assertTrue(matches.isEmpty());
    }

    @Test
    void searchBriefsReturnsEmptyWhenNoMatch() {
        List<Mission> matches = searchService.searchBriefs("nonexistent query");
        assertTrue(matches.isEmpty());
    }
}
