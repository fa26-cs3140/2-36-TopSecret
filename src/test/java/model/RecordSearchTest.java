package model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests for search across all text fields (Member D).
// Disabled until RecordSearch is implemented.
@Disabled("RecordSearch is not implemented yet")
class RecordSearchTest {

    private Connection connection;
    private ModelDatabase db;
    private RecordSearch search;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new ModelDatabase(connection);
        db.createTables();
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE missions (id INTEGER PRIMARY KEY, title TEXT, brief TEXT, date TEXT)");
            s.executeUpdate("INSERT INTO missions VALUES (1, 'Operation Skyfall', 'Recover the stolen drive', '2010-11-12')");
            s.executeUpdate("INSERT INTO missions VALUES (2, 'Black Sky', 'Watch the harbor at night', '2005-09-12')");
        }
        int langley = db.addFacility("Langley Station", "LGS");
        db.addFacility("Harbor Outpost", "HBO");
        db.addAgent("Ghost", "1980-04-02", null, "Expert in disguise", langley);
        db.addAgent("Vanguard", "1975-01-20", "2019-06-30", null, langley);
        db.addBrief("Project Bluebird", "1974-09-09", "Notes on the radio tower");
        db.addBrief("Harbor Report", "2005-09-10", "Ships seen near the outpost");
        search = new RecordSearch(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    private List<String> lines(String term) throws Exception {
        return search.search(term).stream().map(SearchResult::toString).toList();
    }

    @Test
    void findsMissionByTitle() throws Exception {
        assertEquals(List.of("Mission #1: Operation Skyfall"), lines("skyfall"));
    }

    @Test
    void findsMissionByBriefText() throws Exception {
        assertEquals(List.of("Mission #1: Operation Skyfall"), lines("stolen drive"));
    }

    @Test
    void findsMissionByDate() throws Exception {
        assertEquals(List.of("Mission #1: Operation Skyfall"), lines("2010-11"));
    }

    @Test
    void findsBriefByTitleAndByText() throws Exception {
        assertEquals(List.of("Brief #1: Project Bluebird"), lines("bluebird"));
        assertEquals(List.of("Brief #1: Project Bluebird"), lines("radio tower"));
    }

    @Test
    void findsAgentByNameAndByNotes() throws Exception {
        assertEquals(List.of("Agent #1: Ghost"), lines("ghost"));
        assertEquals(List.of("Agent #1: Ghost"), lines("disguise"));
    }

    @Test
    void findsAgentByDates() throws Exception {
        assertEquals(List.of("Agent #2: Vanguard"), lines("1975-01-20"));
        assertEquals(List.of("Agent #2: Vanguard"), lines("2019-06-30"));
    }

    @Test
    void findsFacilityByNameAndByAbbreviation() throws Exception {
        assertEquals(List.of("Facility #1: Langley Station"), lines("langley"));
        assertEquals(List.of("Facility #1: Langley Station"), lines("LGS"));
    }

    @Test
    void searchIgnoresCaseAndSurroundingSpaces() throws Exception {
        assertEquals(List.of("Agent #1: Ghost"), lines("  GHOST  "));
    }

    @Test
    void oneTermCanMatchEveryTypeInOrder() throws Exception {
        // "harbor" is in a mission brief, a brief title, and a facility name
        assertEquals(List.of("Mission #2: Black Sky", "Brief #2: Harbor Report",
                "Facility #2: Harbor Outpost"), lines("harbor"));
    }

    @Test
    void recordIsListedOnceWhenSeveralFieldsMatch() throws Exception {
        // "sky" is in both mission titles
        assertEquals(List.of("Mission #1: Operation Skyfall", "Mission #2: Black Sky"), lines("sky"));
        // "outpost" is in facility 2's name and brief 2's text: one line each
        assertEquals(List.of("Brief #2: Harbor Report", "Facility #2: Harbor Outpost"), lines("outpost"));
    }

    @Test
    void resultCarriesTypeIdAndTitle() throws Exception {
        SearchResult result = search.search("vanguard").get(0);

        assertEquals(RecordSearch.AGENT, result.getType());
        assertEquals(2, result.getId());
        assertEquals("Vanguard", result.getTitle());
    }

    @Test
    void emptyFieldsAreSkipped() throws Exception {
        // Vanguard has no notes and Ghost has no date of death
        assertTrue(lines("null").isEmpty());
    }

    @Test
    void blankOrMissingTermFindsNothing() throws Exception {
        assertTrue(search.search(null).isEmpty());
        assertTrue(search.search("").isEmpty());
        assertTrue(search.search("   ").isEmpty());
    }

    @Test
    void noMatchGivesEmptyList() throws Exception {
        assertTrue(search.search("zzz-not-here").isEmpty());
    }

    @Test
    void percentAndUnderscoreAreOrdinaryCharacters() throws Exception {
        assertTrue(search.search("%").isEmpty());
        assertTrue(search.search("_").isEmpty());
    }
}
