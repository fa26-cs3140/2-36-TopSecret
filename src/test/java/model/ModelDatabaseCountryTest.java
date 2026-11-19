package model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelDatabaseCountryTest {

    private Connection connection;
    private ModelDatabase db;

    @TempDir
    Path folder;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        db = new ModelDatabase(connection);
        db.createTables();
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    // ---------- Setup helpers (plain SQL, so these tests do not depend on Member C's methods) ----------

    private int insertCountry(String name) throws Exception {
        try (PreparedStatement p = connection.prepareStatement(
                "INSERT INTO countries(name) VALUES (?)")) {
            p.setString(1, name);
            p.executeUpdate();
        }
        return lastId(connection);
    }

    private int insertFacility(String name, String abbreviation, Integer countryId) throws Exception {
        try (PreparedStatement p = connection.prepareStatement(
                "INSERT INTO facilities(name, abbreviation, country_id) VALUES (?,?,?)")) {
            p.setString(1, name);
            p.setString(2, abbreviation);
            p.setObject(3, countryId);
            p.executeUpdate();
        }
        return lastId(connection);
    }

    private static int lastId(Connection c) throws Exception {
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT last_insert_rowid()")) {
            r.next();
            return r.getInt(1);
        }
    }

    private void writeCountries(String... lines) throws Exception {
        Files.write(folder.resolve("countries.tsv"),
                List.of(lines), StandardCharsets.UTF_8);
    }

    // ---------- Reading countries ----------

    @Test
    void newDatabaseHasNoCountries() throws Exception {
        assertTrue(db.getCountries().isEmpty());
    }

    @Test
    void createTablesCanRunTwiceWithoutLosingCountries() throws Exception {
        insertCountry("Norway");
        db.createTables();
        assertEquals(1, db.getCountries().size());
    }

    @Test
    void getCountriesReturnsAllInIdOrder() throws Exception {
        int a = insertCountry("Norway");
        int b = insertCountry("Chile");
        List<Country> all = db.getCountries();
        assertEquals(2, all.size());
        assertEquals(a, all.get(0).getId());
        assertEquals("Norway", all.get(0).getName());
        assertEquals(b, all.get(1).getId());
    }

    @Test
    void getCountryFindsById() throws Exception {
        int id = insertCountry("Norway");
        Country c = db.getCountry(id);
        assertNotNull(c);
        assertEquals(id, c.getId());
        assertEquals("Norway", c.getName());
    }

    @Test
    void getCountryReturnsNullWhenMissing() throws Exception {
        assertNull(db.getCountry(999));
    }

    // ---------- A facility shows its country ----------

    @Test
    void getFacilitiesFillsInTheCountryId() throws Exception {
        int country = insertCountry("Norway");
        insertFacility("Oslo Station", "OSL", country);
        assertEquals(country, db.getFacilities().get(0).getCountryId());
    }

    @Test
    void facilityWithNoCountryHasCountryIdZero() throws Exception {
        insertFacility("Langley", "LNG", null);
        assertEquals(0, db.getFacilities().get(0).getCountryId());
    }

    @Test
    void facilityAddedTheOldWayHasNoCountry() throws Exception {
        db.addFacility("Langley", "LNG");
        Facility f = db.getFacilities().get(0);
        assertEquals(0, f.getCountryId());
        assertNull(db.getCountryForFacility(f.getId()));
    }

    @Test
    void getCountryForFacilityReturnsTheCountry() throws Exception {
        int country = insertCountry("Norway");
        int fid = insertFacility("Oslo Station", "OSL", country);
        Country c = db.getCountryForFacility(fid);
        assertNotNull(c);
        assertEquals("Norway", c.getName());
    }

    @Test
    void getCountryForFacilityReturnsNullWhenFacilityHasNone() throws Exception {
        int fid = insertFacility("Langley", "LNG", null);
        assertNull(db.getCountryForFacility(fid));
    }

    @Test
    void getCountryForFacilityReturnsNullForUnknownFacility() throws Exception {
        assertNull(db.getCountryForFacility(999));
    }

    @Test
    void anEditedCountryShowsItsNewNameAtTheFacility() throws Exception {
        int country = insertCountry("Norwy");
        int fid = insertFacility("Oslo Station", "OSL", country);

        try (PreparedStatement p = connection.prepareStatement(
                "UPDATE countries SET name = ? WHERE id = ?")) {
            p.setString(1, "Norway");
            p.setInt(2, country);
            p.executeUpdate();
        }

        assertEquals("Norway", db.getCountryForFacility(fid).getName());
    }

    @Test
    void aCountryCanHaveZeroOrMoreFacilitiesAtOnce() throws Exception {
        int norway = insertCountry("Norway");
        insertCountry("Chile");
        int a = insertFacility("Oslo Station", "OSL", norway);
        int b = insertFacility("Bergen Station", "BRG", norway);

        assertEquals("Norway", db.getCountryForFacility(a).getName());
        assertEquals("Norway", db.getCountryForFacility(b).getName());
    }

    // ---------- Upgrading a database from before countries existed ----------

    @Test
    void oldDatabaseWithoutCountryColumnIsUpgraded() throws Exception {
        try (Connection old = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement s = old.createStatement()) {
                s.executeUpdate("CREATE TABLE facilities (id INTEGER PRIMARY KEY, "
                        + "name TEXT NOT NULL, abbreviation TEXT NOT NULL)");
                s.executeUpdate("INSERT INTO facilities(name, abbreviation) "
                        + "VALUES ('Old Base', 'OLD')");
            }
            ModelDatabase upgraded = new ModelDatabase(old);
            upgraded.createTables();

            List<Facility> all = upgraded.getFacilities();
            assertEquals(1, all.size());
            assertEquals("Old Base", all.get(0).getName());
            assertEquals(0, all.get(0).getCountryId());
            assertTrue(upgraded.getCountries().isEmpty());
        }
    }

    // ---------- Importing the supplied country list ----------

    @Test
    void importCountriesLoadsEveryName() throws Exception {
        writeCountries("Norway", "Chile", "Japan");
        assertEquals(3, db.importCountries(folder));
        assertEquals(3, db.getCountries().size());
        assertEquals("Norway", db.getCountries().get(0).getName());
    }

    @Test
    void importCountriesSkipsBlankLinesAndTrimsNames() throws Exception {
        writeCountries("  Norway  ", "", "   ", "Chile");
        assertEquals(2, db.importCountries(folder));
        assertEquals("Norway", db.getCountries().get(0).getName());
    }

    @Test
    void importCountriesSkipsRepeatedNamesInTheFile() throws Exception {
        writeCountries("Norway", "Chile", "Norway");
        assertEquals(2, db.importCountries(folder));
        assertEquals(2, db.getCountries().size());
    }

    @Test
    void importCountriesIsSafeToRunEveryTime() throws Exception {
        writeCountries("Norway", "Chile");
        db.importCountries(folder);
        assertEquals(0, db.importCountries(folder));
        assertEquals(2, db.getCountries().size());
    }

    @Test
    void importCountriesDoesNotTouchAnExistingList() throws Exception {
        insertCountry("Atlantis");
        writeCountries("Norway", "Chile");
        assertEquals(0, db.importCountries(folder));
        assertEquals(1, db.getCountries().size());
    }

    @Test
    void importCountriesWithNoFileAddsNothing() throws Exception {
        assertEquals(0, db.importCountries(folder));
        assertTrue(db.getCountries().isEmpty());
    }

    @Test
    void importSampleDataAlsoImportsCountries() throws Exception {
        writeCountries("Norway", "Chile", "Japan");
        db.importSampleData(folder);
        assertEquals(3, db.getCountries().size());
    }

    @Test
    void importSampleDataTwiceDoesNotDuplicateCountries() throws Exception {
        writeCountries("Norway", "Chile", "Japan");
        db.importSampleData(folder);
        db.importSampleData(folder);
        assertEquals(3, db.getCountries().size());
    }
}