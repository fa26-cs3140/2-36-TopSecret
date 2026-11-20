package controller;

import model.MemorySessionStore;
import model.SessionStore;
import model.UserAccount;
import model.UserStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// HW7 web server behaviour over real HTTP: the 30 minute session timeout,
// deleted sessions, the stylesheet, and routing of record pages and forms.
class WebControllerSessionTest {

    private final List<UserAccount> saved = new ArrayList<>();
    private final TestClock clock = new TestClock(Instant.parse("2026-11-16T12:00:00Z"));
    private final SessionStore sessions = new MemorySessionStore(clock);
    private final List<String> handled = new ArrayList<>();
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).build();

    private Map<String, String> lastParams;
    private WebController controller;

    // Pages that answer with simple text so the routing can be checked
    private final WebController.Pages pages = new WebController.Pages() {
        @Override
        public String render(String path, UserAccount user) {
            return "<p>page " + path + " for " + user.getUsername() + "</p>";
        }

        @Override
        public PageResult handle(String method, String path,
                                 Map<String, String> params, UserAccount user) {
            handled.add(method + " " + path);
            lastParams = params;
            if (path.equals("/agents/404")) {
                return null;
            }
            if (method.equals("POST")) {
                return PageResult.redirectTo("/agents/7");
            }
            if (path.equals("/agents/500")) {
                return PageResult.error(400, "Edit agent", "<p>Name is required</p>");
            }
            return PageResult.page("Ghost", "<p>record " + path + "</p>");
        }
    };

    @BeforeEach
    void setUp() {
        AccountManager accounts = new AccountManager(new UserStore() {
            @Override
            public List<UserAccount> loadAll() {
                return new ArrayList<>(saved);
            }

            @Override
            public void saveAll(List<UserAccount> users) {
                saved.clear();
                saved.addAll(users);
            }
        });
        UserAccount admin = accounts.createFirstAccount("Admin", "Hello123");
        accounts.addUser(admin, "Lina");
        accounts.setInitialPassword("Lina", "Hello123", "Hello123");
        controller = new WebController(accounts, pages, 0, sessions);
        controller.start();
    }

    @AfterEach
    void tearDown() {
        controller.stop();
    }

    private HttpResponse<String> request(String method, String path, String body, String cookie)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create("http://localhost:" + controller.getPort() + path));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        builder.header("Content-Type", "application/x-www-form-urlencoded");
        builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        return request("GET", path, "", cookie);
    }

    private String login(String username) throws Exception {
        HttpResponse<String> response = request("POST", "/login",
                "username=" + username + "&password=Hello123", null);
        assertEquals(303, response.statusCode());
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    // ---------- Session timeout ----------

    @Test
    void sessionWorksBeforeThirtyMinutes() throws Exception {
        String cookie = login("Lina");

        clock.advance(Duration.ofMinutes(29));

        assertEquals(200, get("/agents", cookie).statusCode());
    }

    @Test
    void sessionTimesOutAfterThirtyIdleMinutes() throws Exception {
        String cookie = login("Lina");

        clock.advance(Duration.ofMinutes(30));

        HttpResponse<String> response = get("/agents", cookie);
        assertEquals(303, response.statusCode());
        assertEquals("/login", response.headers().firstValue("Location").orElseThrow());
    }

    @Test
    void eachRequestResetsTheTimer() throws Exception {
        String cookie = login("Lina");

        clock.advance(Duration.ofMinutes(20));
        assertEquals(200, get("/agents", cookie).statusCode());
        clock.advance(Duration.ofMinutes(20));

        assertEquals(200, get("/agents", cookie).statusCode());
    }

    @Test
    void loginIsListedWithItsToken() throws Exception {
        String cookie = login("Lina");

        assertEquals(1, sessions.list().size());
        assertEquals("Lina", sessions.list().get(0).getUsername());
        assertEquals("SESSION=" + sessions.list().get(0).getToken(), cookie);
    }

    @Test
    void deletedSessionMustLogInAgain() throws Exception {
        String cookie = login("Lina");
        assertEquals(200, get("/agents", cookie).statusCode());

        sessions.delete(sessions.list().get(0).getToken());

        assertEquals(303, get("/agents", cookie).statusCode());
    }

    @Test
    void sessionFromBeforeARestartStillWorks() throws Exception {
        // The session is in the store, but this server has not seen the login
        String token = sessions.create("Lina");

        HttpResponse<String> response = get("/agents", "SESSION=" + token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("for Lina"));
    }

    @Test
    void sessionForARemovedAccountIsRejected() throws Exception {
        String token = sessions.create("nobody");

        assertEquals(303, get("/agents", "SESSION=" + token).statusCode());
    }

    @Test
    void loggingInAgainReplacesTheOldSession() throws Exception {
        String first = login("Lina");
        HttpResponse<String> again = request("POST", "/login",
                "username=Lina&password=Hello123", first);

        assertEquals(303, again.statusCode());
        assertEquals(1, sessions.list().size());
        assertEquals(303, get("/agents", first).statusCode());
    }

    @Test
    void logOutLinkEndsTheSession() throws Exception {
        String cookie = login("Lina");

        assertEquals(303, get("/logout", cookie).statusCode());

        assertTrue(sessions.list().isEmpty());
        assertEquals(303, get("/agents", cookie).statusCode());
        assertEquals(405, request("PUT", "/logout", "", cookie).statusCode());
    }

    // ---------- Styling and menu ----------

    @Test
    void stylesheetIsServedWithoutLogin() throws Exception {
        HttpResponse<String> response = get("/style.css", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("text/css"));
        assertTrue(response.body().contains("nav"));
    }

    @Test
    void everyPageHasTheMenuWithLogOutAndTheStylesheet() throws Exception {
        String cookie = login("Lina");

        for (String path : List.of("/", "/agents", "/countries", "/agents/7")) {
            String body = get(path, cookie).body();
            assertTrue(body.contains("<nav"), path);
            assertTrue(body.contains("href=\"/logout\""), path);
            assertTrue(body.contains("href=\"/style.css\""), path);
            assertFalse(body.contains("<script"), path);
            assertFalse(body.contains("<img"), path);
        }
        assertTrue(get("/login", null).body().contains("href=\"/style.css\""));
    }

    @Test
    void onlyAdminsSeeTheAuditLinkInTheMenu() throws Exception {
        assertTrue(get("/", login("Admin")).body().contains("href=\"/audit\""));
        assertFalse(get("/", login("Lina")).body().contains("href=\"/audit\""));
    }

    @Test
    void countriesListIsARealPage() throws Exception {
        HttpResponse<String> response = get("/countries", login("Lina"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("page /countries for Lina"));
    }

    // ---------- Record pages and forms ----------

    @Test
    void recordDetailsAreShownInsideThePage() throws Exception {
        HttpResponse<String> response = get("/agents/7", login("Lina"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("record /agents/7"));
        assertTrue(response.body().contains("<h2>Ghost</h2>"));
        assertEquals(List.of("GET /agents/7"), handled);
    }

    @Test
    void recordPagesNeedALogin() throws Exception {
        assertEquals(303, get("/agents/7", null).statusCode());
        assertEquals(303, get("/agents/new", null).statusCode());
        assertEquals(303, request("POST", "/agents/new", "name=x", null).statusCode());
        assertTrue(handled.isEmpty());
    }

    @Test
    void unknownRecordIsNotFound() throws Exception {
        assertEquals(404, get("/agents/404", login("Lina")).statusCode());
    }

    @Test
    void standardUserCannotOpenOrSubmitAnyForm() throws Exception {
        String cookie = login("Lina");

        for (String path : List.of("/agents/new", "/briefs/edit?id=1", "/missions/new",
                "/countries/edit?id=1", "/facilities/new")) {
            assertEquals(403, get(path, cookie).statusCode(), path);
            assertEquals(403, request("POST", path, "name=x", cookie).statusCode(), path);
        }
        assertTrue(handled.isEmpty());
    }

    @Test
    void adminGetsTheFormAndItsErrorStatus() throws Exception {
        String cookie = login("Admin");

        assertEquals(200, get("/agents/new", cookie).statusCode());
        HttpResponse<String> invalid = get("/agents/500", cookie);
        assertEquals(400, invalid.statusCode());
        assertTrue(invalid.body().contains("Name is required"));
    }

    @Test
    void adminPostIsSavedAndRedirected() throws Exception {
        HttpResponse<String> response = request("POST", "/agents/edit?id=7",
                "name=Ghost+II&notes=a%26b", login("Admin"));

        assertEquals(303, response.statusCode());
        assertEquals("/agents/7", response.headers().firstValue("Location").orElseThrow());
        assertEquals(List.of("POST /agents/edit"), handled);
        assertEquals("7", lastParams.get("id"));
        assertEquals("Ghost II", lastParams.get("name"));
        assertEquals("a&b", lastParams.get("notes"));
    }

    @Test
    void oversizedFormIsRejected() throws Exception {
        HttpResponse<String> response = request("POST", "/agents/new",
                "name=" + "x".repeat(8200), login("Admin"));

        assertEquals(413, response.statusCode());
        assertTrue(handled.isEmpty());
    }

    @Test
    void postIsOnlyAllowedOnForms() throws Exception {
        String cookie = login("Admin");

        assertEquals(405, request("POST", "/agents/7", "", cookie).statusCode());
        assertEquals(405, request("POST", "/countries", "", cookie).statusCode());
    }

    // A clock the tests can move forward to check the timeout
    private static class TestClock extends Clock {
        private Instant now;

        TestClock(Instant start) {
            now = start;
        }

        void advance(Duration amount) {
            now = now.plus(amount);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
