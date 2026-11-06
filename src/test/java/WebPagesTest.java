import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebPagesTest {

    private Connection connection;
    private MissionStore store;
    private AuditLog audit;
    private WebPages pages;

    private final UserAccount user =
            new UserAccount("Lina", "Hello123", false);

    @BeforeEach
    void setup() throws Exception {
        connection = DriverManager.getConnection(
                "jdbc:sqlite::memory:");

        new ModelDatabase(connection).createTables();

        store = mock(MissionStore.class);
        audit = mock(AuditLog.class);

        pages = new WebPages(connection, store, audit);
    }

    @AfterEach
    void cleanup() throws Exception {
        connection.close();
    }

    @Test
    void rendersDatabaseRecordsAndLogsReads() throws Exception {
        ModelDatabase database = new ModelDatabase(connection);

        int facilityId = database.addFacility("HQ <private>", "HQ");

        database.addAgent(
                "Ghost", "1980-01-01",
                null, null, facilityId);

        database.addBrief(
                "Alpha", "2026-11-05", "Secret <data>");

        assertTrue(pages.render("/facilities", user)
                .contains("HQ &lt;private&gt;"));

        assertTrue(pages.render("/agents", user)
                .contains("Ghost"));

        assertTrue(pages.render("/briefs", user)
                .contains("Secret &lt;data&gt;"));

        verify(audit).record(
                "Lina", AuditLog.READ, "Web facilities");

        verify(audit).record(
                "Lina", AuditLog.READ, "Web agents");

        verify(audit).record(
                "Lina", AuditLog.READ, "Web briefs");
    }

    @Test
    void rendersMissionAssignments() throws Exception {
        when(store.getAllMissions()).thenReturn(List.of(
                new Mission(
                        1, "Alpha", "Brief", "2026-11-05",
                        2, List.of(3, 4))
        ));

        String html = pages.render("/missions", user);

        assertTrue(html.contains("Alpha"));
        assertTrue(html.contains("3, 4"));

        verify(audit).record(
                "Lina", AuditLog.READ, "Web missions");
    }

    @Test
    void protectsAuditInAdapterToo() {
        assertThrows(
                IllegalStateException.class,
                () -> pages.render("/audit", user));

        verifyNoInteractions(audit);
    }

    @Test
    void rendersAuditForAdministrator() throws Exception {
        when(audit.getEntries()).thenReturn(List.of(
                new AuditEntry(
                        1, "today", "Lina", "READ", "Brief #1")
        ));

        UserAccount admin =
                new UserAccount("Admin", "Hello123", true);

        assertTrue(pages.render("/audit", admin)
                .contains("Brief #1"));
    }

    @Test
    void escapesWelcomeName() throws Exception {
        UserAccount unsafeName =
                new UserAccount("<script>", "Hello123", false);

        assertTrue(pages.render("/", unsafeName)
                .contains("&lt;script&gt;"));
    }
}