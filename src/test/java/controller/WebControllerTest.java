package controller;

import model.UserAccount;
import model.AuditLog;
import model.MissionStore;
import model.ModelDatabase;
import model.SqliteMessageStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import view.WebPages;

import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebControllerTest {

    private AccountManager accounts;
    private WebController.Pages pages;
    private WebController controller;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @BeforeEach
    void setup() {
        accounts = mock(AccountManager.class);
        pages = mock(WebController.Pages.class);

        when(accounts.hasAccounts()).thenReturn(true);

        when(accounts.login("Lina", "Hello123"))
                .thenReturn(new UserAccount(
                        "Lina", "Hello123", false));

        when(accounts.login("lina", "Hello123"))
                .thenReturn(new UserAccount("lina", "Hello123", false));

        when(accounts.login("Admin", "Hello123"))
                .thenReturn(new UserAccount(
                        "Admin", "Hello123", true));

        controller = new WebController(accounts, pages, 0);
    }

    @AfterEach
    void cleanup() {
        controller.stop();
    }

    private HttpResponse<String> request(
            String method, String path,
            String body, String cookie) throws Exception {

        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create("http://localhost:"
                        + controller.getPort() + path));

        if (cookie != null) {
            builder.header("Cookie", cookie);
        }

        builder.header(
                "Content-Type",
                "application/x-www-form-urlencoded");

        builder.method(
                method,
                HttpRequest.BodyPublishers.ofString(body));

        return client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String login(String username) throws Exception {
        HttpResponse<String> response = request(
                "POST",
                "/login",
                "username=" + username + "&password=Hello123",
                null);

        assertEquals(303, response.statusCode());

        return response.headers()
                .firstValue("Set-Cookie")
                .orElseThrow()
                .split(";", 2)[0];
    }

    @Test
    void rejectsStartupWithoutAccounts() {
        when(accounts.hasAccounts()).thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                controller::start);
    }

    @Test
    void validCredentialsReturnUser() {
        assertNotNull(
                controller.authenticate("Lina", "Hello123"));
    }

    @Test
    void invalidCredentialsReturnNull() {
        assertNull(
                controller.authenticate("Lina", "wrong"));
    }

    @Test
    void servesLoginFormAtRoot() throws Exception {
        controller.start();

        HttpResponse<String> response =
                request("GET", "/", "", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("name=\"username\""));
        assertTrue(response.body().contains("type=\"password\""));
    }

    @Test
    void validLoginIssuesSessionCookie() throws Exception {
        controller.start();

        HttpResponse<String> response = request(
                "POST", "/login",
                "username=Lina&password=Hello123", null);

        assertEquals(303, response.statusCode());

        String cookie = response.headers()
                .firstValue("Set-Cookie")
                .orElseThrow();

        assertTrue(cookie.contains("HttpOnly"));
        assertFalse(cookie.toLowerCase().contains("expires="));
        assertFalse(cookie.toLowerCase().contains("max-age="));
    }

    @Test
    void invalidLoginDoesNotIssueCookie() throws Exception {
        controller.start();

        HttpResponse<String> response = request(
                "POST", "/login",
                "username=Lina&password=wrong", null);

        assertEquals(401, response.statusCode());

        assertTrue(response.headers()
                .firstValue("Set-Cookie").isEmpty());
    }

    @Test
    void protectsEveryDataRoute() throws Exception {
        controller.start();

        for (String path : new String[]{
                "/agents", "/facilities",
                "/briefs", "/missions", "/audit"
        }) {
            assertEquals(303,
                    request("GET", path, "", null).statusCode());

            assertEquals(303,
                    request("GET", path, "",
                            "SESSION=forged").statusCode());
        }

        verifyNoInteractions(pages);
    }

    @Test
    void authenticatedUserCanNavigate() throws Exception {
        when(pages.render(eq("/missions"), any()))
                .thenReturn("<p>Mission Alpha</p>");

        controller.start();

        HttpResponse<String> response = request(
                "GET", "/missions", "", login("Lina"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Mission Alpha"));
    }

    @Test
    void standardUserCannotAccessAuditDirectly() throws Exception {
        controller.start();

        assertEquals(403,
                request("GET", "/audit", "",
                        login("Lina")).statusCode());

        verifyNoInteractions(pages);
    }

    @Test
    void adminCanAccessAudit() throws Exception {
        when(pages.render(eq("/audit"), any()))
                .thenReturn("<p>Audit entries</p>");

        controller.start();

        assertEquals(200,
                request("GET", "/audit", "",
                        login("Admin")).statusCode());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        controller.start();

        String cookie = login("Lina");

        assertEquals(303,
                request("POST", "/logout", "", cookie).statusCode());

        assertEquals(303,
                request("GET", "/missions", "", cookie).statusCode());
    }

    @Test
    void rejectsWritesAndUnknownRoutes() throws Exception {
        controller.start();

        String cookie = login("Lina");

        assertEquals(405,
                request("POST", "/agents", "", cookie).statusCode());

        assertEquals(404,
                request("GET", "/missing", "", cookie).statusCode());
    }

    @Test
    void sendsAndReadsMessagesOnlyAsTheRecipient(@TempDir Path dir) throws Exception {
        Path db = dir.resolve("web.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            ModelDatabase model = new ModelDatabase(connection);
            model.createTables();
            int facility = model.addFacility("Test Facility", "TF");
            model.addAgent("lina", null, null, null, facility);
            model.addAgent("Lina", null, null, null, facility);
            var messages = new SqliteMessageStore(db);
            var webPages = new WebPages(connection, mock(MissionStore.class),
                    mock(AuditLog.class), messages);
            controller = new WebController(accounts, webPages, 0);
            controller.start();
            try {
                String form = "agentName=Lina&text=Hello+%26+welcome";
                assertEquals(303, request("POST", "/messages/send", form, null).statusCode());
                assertTrue(messages.getMessagesFor("Lina").isEmpty());

                String sender = login("lina");
                String recipient = login("Lina");
                assertTrue(request("GET", "/", "", sender).body().contains("href=\"/messages\""));
                assertTrue(request("GET", "/messages/new", "", sender).body().contains("<option value=\"Lina\">"));
                assertEquals(405, request("GET", "/messages/send", "", sender).statusCode());
                assertEquals(400, request("POST", "/messages/send", "agentName=Lina&text=+", sender).statusCode());
                var sent = request("POST", "/messages/send", form, sender);
                assertEquals(303, sent.statusCode());
                assertEquals("/messages", sent.headers().firstValue("Location").orElseThrow());

                var message = messages.getMessagesForExactRecipient("Lina").getFirst();
                String detail = "/messages/" + message.getId();
                assertTrue(request("GET", "/messages", "", recipient).body().contains("Unread"));
                assertTrue(request("GET", "/messages", "", sender).body().contains("No messages available."));
                assertTrue(request("GET", detail, "", sender).body().contains("Message not found"));
                assertFalse(messages.getMessage(message.getId(), "Lina").isRead());
                assertTrue(request("GET", detail, "", recipient).body().contains("Hello &amp; welcome"));
                assertTrue(new SqliteMessageStore(db).getMessage(message.getId(), "Lina").isRead());
                assertTrue(request("GET", "/messages", "", recipient).body().contains(">Read<"));
            } finally {
                controller.stop();
            }
        }
    }

    @Test
    void handlesPageFailureWithoutLeakingDetails() throws Exception {
        when(pages.render(eq("/briefs"), any()))
                .thenThrow(new IllegalStateException(
                        "secret database path"));

        controller.start();

        HttpResponse<String> response = request(
                "GET", "/briefs", "", login("Lina"));

        assertEquals(500, response.statusCode());
        assertFalse(response.body().contains("secret database path"));
    }

    @Test
    void rejectsMalformedForm() throws Exception {
        controller.start();

        assertEquals(400,
                request("POST", "/login",
                        "username=%ZZ&password=x",
                        null).statusCode());
    }

    @Test
    void rejectsOversizedForm() throws Exception {
        controller.start();

        assertEquals(413,
                request("POST", "/login",
                        "x".repeat(8193), null).statusCode());
    }
}
