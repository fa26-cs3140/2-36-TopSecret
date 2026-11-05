import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import view.HtmlView;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebController {

    public static final int PORT = 3002;

    public interface Pages {
        String render(String path, UserAccount user) throws Exception;
    }

    private final AccountManager accounts;
    private final Pages pages;
    private final int requestedPort;
    private final HtmlView view = new HtmlView();

    private final Map<String, UserAccount> sessions =
            new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    private HttpServer server;

    public WebController(AccountManager accounts, Pages pages) {
        this(accounts, pages, PORT);
    }

    public WebController(AccountManager accounts, Pages pages, int port) {
        this.accounts = Objects.requireNonNull(accounts);
        this.pages = Objects.requireNonNull(pages);
        this.requestedPort = port;
    }

    public synchronized void start() {
        if (server != null) {
            throw new IllegalStateException("Server already started.");
        }

        if (!accounts.hasAccounts()) {
            throw new IllegalStateException(
                    "Create a user account in terminal mode first.");
        }

        try {
            server = HttpServer.create(
                    new InetSocketAddress("localhost", requestedPort), 0);

            server.createContext("/", this::handle);
            server.start();

        } catch (IOException e) {
            if (server != null) {
                server.stop(0);
            }

            server = null;
            throw new UncheckedIOException("Cannot start web server.", e);
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
        }

        server = null;
        sessions.clear();
    }

    public int getPort() {
        if (server == null) {
            throw new IllegalStateException("Server is not running.");
        }

        return server.getAddress().getPort();
    }

    public UserAccount authenticate(String username, String password) {
        return accounts.login(username, password);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            exchange.getResponseHeaders().set(
                    "Cache-Control", "no-store");

            exchange.getResponseHeaders().set(
                    "X-Content-Type-Options", "nosniff");

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (path.equals("/login")) {
                if (method.equals("GET")) {
                    send(exchange, 200, loginForm(""));
                } else if (method.equals("POST")) {
                    login(exchange);
                } else {
                    methodNotAllowed(exchange, "GET, POST");
                }

                return;
            }

            if (path.equals("/logout")) {
                if (!method.equals("POST")) {
                    methodNotAllowed(exchange, "POST");
                    return;
                }

                String token = sessionToken(exchange);

                if (token != null) {
                    sessions.remove(token);
                }

                exchange.getResponseHeaders().add(
                        "Set-Cookie",
                        "SESSION=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");

                redirect(exchange, "/login");
                return;
            }

            if (!method.equals("GET")) {
                methodNotAllowed(exchange, "GET");
                return;
            }

            String token = sessionToken(exchange);
            UserAccount user = token == null ? null : sessions.get(token);

            if (user == null) {
                if (path.equals("/")) {
                    send(exchange, 200, loginForm(""));
                } else {
                    redirect(exchange, "/login");
                }

                return;
            }

            if (!Set.of(
                    "/", "/agents", "/facilities",
                    "/briefs", "/missions", "/audit"
            ).contains(path)) {
                send(exchange, 404,
                        view.renderMessage("Page not found."));
                return;
            }

            if (path.equals("/audit") && !user.isAdmin()) {
                send(exchange, 403,
                        view.renderMessage("Administrator access required."));
                return;
            }

            String title = path.equals("/")
                    ? "Home"
                    : path.substring(1);

            String body = pages.render(path, user);

            body += """
                    <form method="post" action="/logout">
                        <button type="submit">Logout</button>
                    </form>
                    """;

            send(exchange, 200,
                    view.renderPage(title, body, user.isAdmin()));

        } catch (IllegalArgumentException e) {
            send(exchange, 400, loginForm("Invalid request."));

        } catch (Exception e) {
            send(exchange, 500,
                    view.renderMessage("Unable to load this page."));

        } finally {
            exchange.close();
        }
    }

    private void login(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(8193);

        if (bytes.length > 8192) {
            send(exchange, 413, loginForm("Form too large."));
            return;
        }

        Map<String, String> form = new HashMap<>();

        String body = new String(bytes, StandardCharsets.UTF_8);

        for (String field : body.split("&")) {
            String[] pair = field.split("=", 2);

            String name = URLDecoder.decode(
                    pair[0], StandardCharsets.UTF_8);

            String value = pair.length == 2
                    ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                    : "";

            form.put(name, value);
        }

        String username = form.get("username");
        String password = form.get("password");

        UserAccount user = username == null || password == null
                ? null
                : authenticate(username, password);

        if (user == null) {
            send(exchange, 401, loginForm(
                    "Incorrect username or password. "
                            + "Set initial passwords in terminal mode."));
            return;
        }

        String oldToken = sessionToken(exchange);

        if (oldToken != null) {
            sessions.remove(oldToken);
        }

        byte[] secret = new byte[32];
        random.nextBytes(secret);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(secret);

        sessions.put(token, user);

        // No Expires or Max-Age: this is a browser-session cookie.
        exchange.getResponseHeaders().add(
                "Set-Cookie",
                "SESSION=" + token
                        + "; Path=/; HttpOnly; SameSite=Lax");

        redirect(exchange, "/");
    }

    private String sessionToken(HttpExchange exchange) {
        for (String header : exchange.getRequestHeaders()
                .getOrDefault("Cookie", List.of())) {

            for (String part : header.split(";")) {
                String[] pair = part.trim().split("=", 2);

                if (pair.length == 2 && pair[0].equals("SESSION")) {
                    return pair[1];
                }
            }
        }

        return null;
    }

    private String loginForm(String error) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>Login</title>
                </head>
                <body>
                    <h1>TOP SECRET SYSTEM</h1>
                """
                + view.renderMessage(error)
                + """
                    <form method="post" action="/login">
                        <label>
                            Username
                            <input name="username"
                                   autocomplete="username" required>
                        </label>
                        <br>
                        <label>
                            Password
                            <input type="password" name="password"
                                   autocomplete="current-password" required>
                        </label>
                        <br>
                        <button type="submit">Login</button>
                    </form>
                </body>
                </html>
                """;
    }

    private void methodNotAllowed(
            HttpExchange exchange, String allow) throws IOException {

        exchange.getResponseHeaders().set("Allow", allow);

        send(exchange, 405,
                view.renderMessage("Method not allowed."));
    }

    private void redirect(
            HttpExchange exchange, String location) throws IOException {

        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
    }

    private void send(
            HttpExchange exchange, int status, String html)
            throws IOException {

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8");

        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}