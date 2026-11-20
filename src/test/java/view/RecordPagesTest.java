package view;

import controller.PageResult;
import model.AuditEntry;
import model.AuditLog;
import model.ModelDatabase;
import model.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Record details, related links, and the add/edit forms, with a real in-memory database (HW7)
class RecordPagesTest {

    private final UserAccount admin = new UserAccount("Admin", "Hello123", true);
    private final UserAccount standard = new UserAccount("Lina", "Hello123", false);
    private final List<String> audit = new ArrayList<>();

    private Connection connection;
    private ModelDatabase db;
    private RecordPages pages;
    private int norway;
    private int langley;
    private int ghost;
    private int report;
    private int skyfall;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new ModelDatabase(connection);
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE missions (id INTEGER PRIMARY KEY, title TEXT, "
                    + "brief TEXT, date TEXT, UNIQUE(title, date))");
        }
        db.createTables();
        norway = db.addCountry("Norway");
        db.addCountry("Chile");
        langley = db.addFacility("Langley Station", "LGS");
        db.setFacilityCountry(langley, norway);
        ghost = db.addAgent("Ghost", "1980-04-02", null, "Expert <b>", langley);
        report = db.addBrief("Harbor Report", "2005-09-10", "Ships <seen>");
        skyfall = db.addMission("Skyfall", "Recover the drive", "2010-11-12", langley);
        db.assignAgent(skyfall, ghost);
        db.attachBrief(skyfall, report);
        pages = new RecordPages(db, new AuditLog() {
            @Override
            public void record(String username, String action, String item) {
                audit.add(username + " " + action + " " + item);
            }

            @Override
            public List<AuditEntry> getEntries() {
                return List.of();
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    private PageResult get(String path, UserAccount user) throws Exception {
        return pages.handle("GET", path, Map.of(), user);
    }

    private PageResult get(String path, String id) throws Exception {
        return pages.handle("GET", path, Map.of("id", id), admin);
    }

    private PageResult post(String path, Map<String, String> fields) throws Exception {
        return pages.handle("POST", path, new HashMap<>(fields), admin);
    }

    // ---------- Which addresses are record pages ----------

    @Test
    void recognisesDetailAndFormAddresses() {
        assertTrue(RecordPages.isDetail("/agents/7"));
        assertTrue(RecordPages.isDetail("/countries/12"));
        assertFalse(RecordPages.isDetail("/agents"));
        assertFalse(RecordPages.isDetail("/agents/x"));
        assertFalse(RecordPages.isDetail("/messages/3"));
        assertTrue(RecordPages.isForm("/facilities/new"));
        assertTrue(RecordPages.isForm("/missions/edit"));
        assertFalse(RecordPages.isForm("/audit/new"));
    }

    @Test
    void otherAddressesAreNotHandled() throws Exception {
        assertNull(get("/agents", admin));
        assertNull(get("/messages/3", admin));
        assertNull(get("/", admin));
    }

    // ---------- Details with related records ----------

    @Test
    void facilityShowsCountryAgentsAndMissionsAsLinks() throws Exception {
        PageResult page = get("/facilities/" + langley, standard);

        assertEquals(200, page.status());
        assertTrue(page.body().contains("Langley Station"));
        assertTrue(page.body().contains("<a href=\"/countries/" + norway + "\">Norway</a>"));
        assertTrue(page.body().contains("<a href=\"/agents/" + ghost + "\">Ghost</a>"));
        assertTrue(page.body().contains("<a href=\"/missions/" + skyfall + "\">Skyfall</a>"));
    }

    @Test
    void agentShowsFacilityAndMissions() throws Exception {
        PageResult page = get("/agents/" + ghost, standard);

        assertEquals("Ghost", page.title());
        assertTrue(page.body().contains("1980-04-02"));
        assertTrue(page.body().contains("Expert &lt;b&gt;"));
        assertTrue(page.body().contains("<a href=\"/facilities/" + langley + "\">Langley Station</a>"));
        assertTrue(page.body().contains("<a href=\"/missions/" + skyfall + "\">Skyfall</a>"));
    }

    @Test
    void missionShowsFacilityAgentsAndBriefs() throws Exception {
        PageResult page = get("/missions/" + skyfall, standard);

        assertEquals("Skyfall", page.title());
        assertTrue(page.body().contains("Recover the drive"));
        assertTrue(page.body().contains("<a href=\"/facilities/" + langley + "\">Langley Station</a>"));
        assertTrue(page.body().contains("<a href=\"/agents/" + ghost + "\">Ghost</a>"));
        assertTrue(page.body().contains("<a href=\"/briefs/" + report + "\">Harbor Report</a>"));
    }

    @Test
    void briefShowsItsMissionAndEscapedText() throws Exception {
        PageResult page = get("/briefs/" + report, standard);

        assertTrue(page.body().contains("Ships &lt;seen&gt;"));
        assertTrue(page.body().contains("<a href=\"/missions/" + skyfall + "\">Skyfall</a>"));
    }

    @Test
    void briefWithoutMissionShowsNone() throws Exception {
        int loose = db.addBrief("Loose", "2026-01-01", "Text");

        assertTrue(get("/briefs/" + loose, standard).body().contains("None"));
    }

    @Test
    void countryShowsItsFacilities() throws Exception {
        PageResult page = get("/countries/" + norway, standard);

        assertEquals("Norway", page.title());
        assertTrue(page.body().contains("<a href=\"/facilities/" + langley + "\">Langley Station</a>"));
    }

    @Test
    void renamedCountryShowsAtItsFacility() throws Exception {
        post("/countries/edit", Map.of("id", String.valueOf(norway), "name", "Kingdom of Norway"));

        assertTrue(get("/facilities/" + langley, standard).body().contains("Kingdom of Norway"));
    }

    @Test
    void viewingARecordIsAudited() throws Exception {
        get("/agents/" + ghost, standard);
        get("/countries/" + norway, standard);

        assertEquals(List.of("Lina READ Agent #" + ghost, "Lina READ Country #" + norway), audit);
    }

    @Test
    void onlyAdminsSeeTheEditLink() throws Exception {
        assertTrue(get("/agents/" + ghost, admin).body().contains("/agents/edit?id=" + ghost));
        assertFalse(get("/agents/" + ghost, standard).body().contains("/agents/edit"));
    }

    @Test
    void missingRecordsAreNotFoundAndNotAudited() throws Exception {
        for (String type : List.of("agents", "facilities", "briefs", "missions", "countries")) {
            assertEquals(404, get("/" + type + "/999", admin).status(), type);
        }
        assertTrue(audit.isEmpty());
    }

    // ---------- Forms ----------

    @Test
    void standardUsersCannotOpenOrSubmitForms() throws Exception {
        assertEquals(403, get("/agents/new", standard).status());
        assertEquals(403, pages.handle("POST", "/countries/new",
                Map.of("name", "Peru"), standard).status());
        assertEquals(2, db.getCountries().size());
    }

    @Test
    void newFormsAreEmptyWithTheRightFields() throws Exception {
        PageResult agent = get("/agents/new", admin);
        assertEquals(200, agent.status());
        assertEquals("New agent", agent.title());
        assertTrue(agent.body().contains("action=\"/agents/new\""));
        assertTrue(agent.body().contains("name=\"name\" value=\"\""));
        assertTrue(agent.body().contains("<option value=\"" + langley + "\">Langley Station</option>"));

        assertTrue(get("/briefs/new", admin).body().contains("name=\"text\""));
        assertTrue(get("/missions/new", admin).body().contains("name=\"brief\""));
        assertTrue(get("/countries/new", admin).body().contains("name=\"name\""));

        String facility = get("/facilities/new", admin).body();
        assertTrue(facility.contains("name=\"abbreviation\""));
        assertTrue(facility.contains("<option value=\"" + norway + "\">Norway</option>"));
    }

    @Test
    void editFormsAreFilledWithCurrentValues() throws Exception {
        String agent = get("/agents/edit", String.valueOf(ghost)).body();
        assertTrue(agent.contains("action=\"/agents/edit?id=" + ghost + "\""));
        assertTrue(agent.contains("value=\"Ghost\""));
        assertTrue(agent.contains("value=\"1980-04-02\""));
        assertTrue(agent.contains("Expert &lt;b&gt;</textarea>"));
        assertTrue(agent.contains("<option value=\"" + langley + "\" selected>"));

        String facility = get("/facilities/edit", String.valueOf(langley)).body();
        assertTrue(facility.contains("value=\"Langley Station\""));
        assertTrue(facility.contains("value=\"LGS\""));
        assertTrue(facility.contains("<option value=\"" + norway + "\" selected>Norway</option>"));

        assertTrue(get("/briefs/edit", String.valueOf(report)).body().contains("value=\"Harbor Report\""));
        assertTrue(get("/missions/edit", String.valueOf(skyfall)).body().contains("Recover the drive</textarea>"));
        assertEquals("Edit country", get("/countries/edit", String.valueOf(norway)).title());
        assertTrue(get("/countries/edit", String.valueOf(norway)).body().contains("value=\"Norway\""));
    }

    @Test
    void editFormForMissingOrBadIdIsNotFound() throws Exception {
        assertEquals(404, get("/agents/edit", "999").status());
        assertEquals(404, get("/facilities/edit", "abc").status());
        assertEquals(404, get("/briefs/edit", "999").status());
        assertEquals(404, get("/missions/edit", "999").status());
        assertEquals(404, get("/countries/edit", "999").status());
        assertEquals(404, pages.handle("GET", "/countries/edit", Map.of(), admin).status());
    }

    // ---------- Saving ----------

    @Test
    void creatingEachTypeSavesRedirectsAndAudits() throws Exception {
        PageResult country = post("/countries/new", Map.of("name", "Peru"));
        assertTrue(country.isRedirect());
        assertEquals("/countries/3", country.redirect());

        PageResult facility = post("/facilities/new",
                Map.of("name", "Lima Post", "abbreviation", "LIM", "countryId", "3"));
        int facilityId = db.getFacilities().get(1).getId();
        assertEquals("/facilities/" + facilityId, facility.redirect());
        assertEquals("Peru", db.getCountryForFacility(facilityId).getName());

        assertTrue(post("/agents/new", Map.of("name", "Echo", "dateOfBirth", "1990-01-01",
                "dateOfDeath", "", "notes", "", "facilityId", String.valueOf(facilityId))).isRedirect());
        assertEquals("Echo", db.getAgents().get(1).getName());

        assertTrue(post("/briefs/new",
                Map.of("title", "New Brief", "date", "2026-01-01", "text", "Body")).isRedirect());
        assertEquals(2, db.getBriefs().size());

        assertTrue(post("/missions/new", Map.of("title", "Night Owl", "date", "2026-02-02",
                "brief", "Watch", "facilityId", String.valueOf(facilityId))).isRedirect());
        assertEquals(2, db.getMissions().size());

        assertEquals(List.of("Admin CREATE Country #3", "Admin CREATE Facility #" + facilityId,
                "Admin CREATE Agent #2", "Admin CREATE Brief #2", "Admin CREATE Mission #2"), audit);
    }

    @Test
    void editingSavesTheChangeAndAuditsIt() throws Exception {
        PageResult result = post("/agents/edit", Map.of("id", String.valueOf(ghost),
                "name", "Ghost II", "dateOfBirth", "1980-04-02", "dateOfDeath", "2030-01-01",
                "notes", "Retired", "facilityId", String.valueOf(langley)));

        assertEquals("/agents/" + ghost, result.redirect());
        assertEquals("Ghost II", db.getAgents().get(0).getName());
        assertEquals("2030-01-01", db.getAgents().get(0).getDateOfDeath());
        assertEquals(List.of("Admin UPDATE Agent #" + ghost), audit);
    }

    @Test
    void editingAFacilityCanChangeItsCountry() throws Exception {
        post("/facilities/edit", Map.of("id", String.valueOf(langley), "name", "Langley Station",
                "abbreviation", "LGS", "countryId", "2"));

        assertEquals("Chile", db.getCountryForFacility(langley).getName());
    }

    @Test
    void editingABriefAndAMission() throws Exception {
        post("/briefs/edit", Map.of("id", String.valueOf(report), "title", "Harbor Report v2",
                "date", "2005-09-11", "text", "Updated"));
        post("/missions/edit", Map.of("id", String.valueOf(skyfall), "title", "Skyfall II",
                "date", "2011-01-01", "brief", "New plan", "facilityId", String.valueOf(langley)));

        assertEquals("Harbor Report v2", db.getBriefs().get(0).getTitle());
        assertEquals("Skyfall II", db.getMissions().get(0).getTitle());
        assertTrue(db.getMissions().get(0).hasAgent(ghost)); // assignments are kept
    }

    @Test
    void missingRequiredFieldShowsTheFormAgainWithTheError() throws Exception {
        PageResult result = post("/agents/new",
                Map.of("name", " ", "notes", "keep me", "facilityId", String.valueOf(langley)));

        assertEquals(400, result.status());
        assertTrue(result.body().contains("Name is required"));
        assertTrue(result.body().contains("keep me</textarea>"));
        assertEquals(1, db.getAgents().size());
        assertTrue(audit.isEmpty());
    }

    @Test
    void duplicateCountryNameShowsAnError() throws Exception {
        PageResult result = post("/countries/new", Map.of("name", "norway"));

        assertEquals(400, result.status());
        assertTrue(result.body().contains("Could not save"));
        assertTrue(result.body().contains("value=\"norway\""));
    }

    @Test
    void savingAMissingRecordShowsAnError() throws Exception {
        PageResult result = post("/countries/edit", Map.of("id", "999", "name", "Nowhere"));

        assertEquals(400, result.status());
        assertTrue(result.body().contains("No such country: 999"));
    }

    @Test
    void nonNumericFacilityIsTreatedAsMissing() throws Exception {
        PageResult result = post("/agents/new", Map.of("name", "Echo", "facilityId", "abc"));

        assertEquals(400, result.status());
        assertTrue(result.body().contains("Facility is required"));
    }
}
