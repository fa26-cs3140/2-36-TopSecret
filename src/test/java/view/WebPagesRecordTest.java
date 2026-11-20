package view;

import controller.PageResult;
import model.AuditEntry;
import model.AuditLog;
import model.ModelDatabase;
import model.SqliteMissionStore;
import model.UserAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// HW7 integration: the list pages link to each record, countries are listed,
// and record pages are reached through WebPages. Uses a real database file.
class WebPagesRecordTest {

    @TempDir
    Path dir;

    private final UserAccount admin = new UserAccount("Admin", "Hello123", true);
    private final UserAccount standard = new UserAccount("Lina", "Hello123", false);
    private final List<String> audit = new ArrayList<>();

    private final AuditLog log = new AuditLog() {
        @Override
        public void record(String username, String action, String item) {
            audit.add(username + " " + action + " " + item);
        }

        @Override
        public List<AuditEntry> getEntries() {
            return List.of();
        }
    };

    private interface Check {
        void run(WebPages pages, ModelDatabase db) throws Exception;
    }

    // Builds a database with one of everything, then runs the check
    private void withPages(Check check) throws Exception {
        Path file = dir.resolve("web.db");
        SqliteMissionStore store = new SqliteMissionStore(file);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file)) {
            ModelDatabase db = new ModelDatabase(connection);
            db.createTables();
            int norway = db.addCountry("Norway");
            int langley = db.addFacility("Langley Station", "LGS");
            db.setFacilityCountry(langley, norway);
            int ghost = db.addAgent("Ghost", null, null, null, langley);
            db.addBrief("Harbor Report", "2005-09-10", "Ships seen");
            int skyfall = db.addMission("Skyfall", "Recover the drive", "2010-11-12", langley);
            db.assignAgent(skyfall, ghost);
            check.run(new WebPages(connection, store, log), db);
        }
    }

    @Test
    void listsLinkEachRecordToItsPage() throws Exception {
        withPages((pages, db) -> {
            assertTrue(pages.render("/agents", standard).contains("<a href=\"/agents/1\">Ghost</a>"));
            assertTrue(pages.render("/facilities", standard)
                    .contains("<a href=\"/facilities/1\">Langley Station</a>"));
            assertTrue(pages.render("/briefs", standard)
                    .contains("<a href=\"/briefs/1\">Harbor Report</a>"));
            assertTrue(pages.render("/missions", standard)
                    .contains("<a href=\"/missions/1\">Skyfall</a>"));
        });
    }

    @Test
    void listsShowRelatedNames() throws Exception {
        withPages((pages, db) -> {
            assertTrue(pages.render("/agents", standard).contains("<td>Langley Station</td>"));
            assertTrue(pages.render("/facilities", standard).contains("<td>Norway</td>"));
            // the mission row shows its facility id and assigned agent id
            assertTrue(pages.render("/missions", standard).contains("<td>1</td><td>1</td></tr>"));
        });
    }

    @Test
    void countriesAreListedWithLinksAndAudited() throws Exception {
        withPages((pages, db) -> {
            String html = pages.render("/countries", standard);

            assertTrue(html.contains("<a href=\"/countries/1\">Norway</a>"));
            assertEquals(List.of("Lina READ Web countries"), audit);
        });
    }

    @Test
    void onlyAdminsGetTheAddLink() throws Exception {
        withPages((pages, db) -> {
            assertTrue(pages.render("/agents", admin).contains("href=\"/agents/new\""));
            assertTrue(pages.render("/countries", admin).contains("href=\"/countries/new\""));
            assertFalse(pages.render("/agents", standard).contains("/agents/new"));
        });
    }

    @Test
    void recordPagesAreReachedThroughHandle() throws Exception {
        withPages((pages, db) -> {
            PageResult detail = pages.handle("GET", "/facilities/1", Map.of(), standard);
            assertEquals(200, detail.status());
            assertTrue(detail.body().contains("Norway"));

            PageResult saved = pages.handle("POST", "/countries/new", Map.of("name", "Peru"), admin);
            assertEquals("/countries/2", saved.redirect());
            assertEquals(2, db.getCountries().size());

            assertNull(pages.handle("GET", "/agents", Map.of(), standard));
        });
    }
}
