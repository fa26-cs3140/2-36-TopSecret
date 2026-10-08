import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SqliteMissionStoreTest {

    @TempDir
    Path tempDir;

    SqliteMissionStore store; // a fresh, empty database for every test
    Path sampleFile;          // a small TSV with 3 missions

    @BeforeEach
    void setUp() throws IOException {
        store = new SqliteMissionStore(tempDir.resolve("test.db"));
        sampleFile = writeFile("Title\tDate\tText\n"
                + "Operation One\t1970-01-01\tBug the diplomatic pouch.\n"
                + "Operation Two\t1971-02-02\tFollow the KREMLIN courier.\n"
                + "Operation Three\t1972-03-03\tMeet the asset at midnight.\n");
    }

    private Path writeFile(String text) throws IOException {
        return Files.writeString(tempDir.resolve("file" + text.hashCode() + ".tsv"), text);
    }

    // ----- database setup -----

    @Test
    void newDatabaseIsEmpty() {
        assertTrue(store.getAllMissions().isEmpty());
    }

    // ----- importFromTsv -----

    @Test
    void importAddsMissions() {
        assertEquals(3, store.importFromTsv(sampleFile));
        assertEquals(3, store.getAllMissions().size());
    }

    @Test
    void importingTwiceDoesNotDuplicate() {
        store.importFromTsv(sampleFile);
        assertEquals(0, store.importFromTsv(sampleFile));
        assertEquals(3, store.getAllMissions().size());
    }

    @Test
    void sameTitleWithDifferentDateIsKept() throws IOException {
        Path file = writeFile("Title\tDate\tText\n"
                + "Project Bluebird\t1973-04-20\tFirst brief\n"
                + "Project Bluebird\t1974-09-09\tSecond brief\n");
        assertEquals(2, store.importFromTsv(file));
    }

    @Test
    void badLinesAreSkipped() throws IOException {
        Path file = writeFile("Title\tDate\tText\n"
                + "Good\t2000-01-01\tgood brief\n"
                + "this line has no tabs\n");
        assertEquals(1, store.importFromTsv(file));
    }

    @Test
    void missingFileThrows() {
        assertThrows(IllegalStateException.class,
                () -> store.importFromTsv(tempDir.resolve("missing.tsv")));
    }

    // ----- getAllMissions / getMission -----

    @Test
    void getAllMissionsReturnsThemInOrder() {
        store.importFromTsv(sampleFile);
        assertEquals("Operation One", store.getAllMissions().get(0).getTitle());
        assertEquals("Operation Three", store.getAllMissions().get(2).getTitle());
    }

    @Test
    void getMissionReturnsTheMissionWithThatId() {
        store.importFromTsv(sampleFile);
        int id = store.getAllMissions().get(0).getId();

        Mission mission = store.getMission(id);

        assertEquals("Operation One", mission.getTitle());
        assertEquals("1970-01-01", mission.getDate());
        assertEquals("Bug the diplomatic pouch.", mission.getBrief());
    }

    @Test
    void getMissionWithUnknownIdReturnsNull() {
        store.importFromTsv(sampleFile);
        assertNull(store.getMission(9999));
    }

    // ----- searchBriefs -----

    @Test
    void searchFindsBriefAndIgnoresCase() {
        store.importFromTsv(sampleFile);
        assertEquals(1, store.searchBriefs("kremlin").size()); // brief says KREMLIN
    }

    @Test
    void searchDoesNotCheckTitles() {
        store.importFromTsv(sampleFile);
        assertTrue(store.searchBriefs("Operation").isEmpty());
    }

    @Test
    void searchWithNoMatchOrBlankTermReturnsEmptyList() {
        store.importFromTsv(sampleFile);
        assertTrue(store.searchBriefs("zzzzqq").isEmpty());
        assertTrue(store.searchBriefs("  ").isEmpty());
    }
}