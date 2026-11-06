package controller;

import model.UserAccount;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.*;

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