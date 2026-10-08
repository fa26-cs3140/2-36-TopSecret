import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;


class MissionStoreIntegrationTest {

    private static final Path REAL_TSV = SqliteMissionStore.DEFAULT_TSV_FILE;

    @TempDir
    Path tempDir;

    @Test
    void importLoadsAllMissionsFromRealFile() {
        MissionStore store = new SqliteMissionStore(tempDir.resolve("test.db"));

        store.importFromTsv(REAL_TSV);

        assertEquals(100, store.getAllMissions().size());
    }

    @Test
    void importThenListReadAndSearch() {
        MissionStore store = new SqliteMissionStore(tempDir.resolve("test.db"));
        store.importFromTsv(REAL_TSV);

        Mission first = store.getAllMissions().get(0);                     // list
        assertEquals("Operation Sandtrap", first.getTitle());
        assertEquals(first.getBrief(), store.getMission(first.getId()).getBrief());            // read one
        assertEquals("Operation Able Archer",
                store.searchBriefs("kremlin").get(0).getTitle());          // search
    }

    @Test
    void dataSurvivesRestartWithoutDuplicates() {
        Path db = tempDir.resolve("test.db");
        new SqliteMissionStore(db).importFromTsv(REAL_TSV);

        MissionStore restarted = new SqliteMissionStore(db); // same file, new object

        assertEquals(100, restarted.getAllMissions().size());
        assertEquals(0, restarted.importFromTsv(REAL_TSV));
    }
}