package controller;

import model.MemorySessionStore;
import model.SessionStore;
import model.UserAccount;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import view.HtmlView;
import view.RecordPages;
import view.WebLayout;
import view.WebPages;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebController {

    public static final int PORT = 3002;

    public interface Pages {
        String render(String path, UserAccount user) throws Exception;

        // HW7: pages that need more than an address: one record's details and the
        // add/edit forms. params holds the query string and any posted form fields.
        // Returns null when this is not such a page.
        default PageResult handle(String method, String path,
                                  Map<String, String> params,
                                  UserAccount user) throws Exception {
            return null;
        }
    }

    private final AccountManager accounts;
    private final Pages pages;
    private final int requestedPort;
    private final HtmlView view = new HtmlView();

    // HW7 access control: which tokens are logged in and when they were last
    // used. A session ends after 30 minutes without a request, or when an
    // admin deletes its token from the terminal.
    private final SessionStore sessions;

    // The account behind each token, so it is not looked up on every request
    private final Map<String, UserAccount> users =
            new ConcurrentHashMap<>();

    private HttpServer server;

    public WebController(AccountManager accounts, Pages pages) {
        this(accounts, pages, PORT);
    }

    public WebController(AccountManager accounts, Pages pages, int port) {
        this(accounts, pages, port, new MemorySessionStore());
    }

    public WebController(AccountManager accounts, Pages pages, int port,
                         SessionStore sessions) {
        this.accounts = Objects.requireNonNull(accounts);
        this.pages = Objects.requireNonNull(pages);
        this.requestedPort = port;
        this.sessions = Objects.requireNonNull(sessions);
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
        users.clear();
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

            if (path.equals(WebLayout.STYLESHEET_PATH)) {
                sendStylesheet(exchange);
                return;
            }

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
                // The menu's log out link sends GET; a form may send POST
                if (!method.equals("POST") && !method.equals("GET")) {
                    methodNotAllowed(exchange, "GET, POST");
                    return;
                }

                String token = sessionToken(exchange);

                if (token != null) {
                    sessions.delete(token);
                    users.remove(token);
                }

                exchange.getResponseHeaders().add(
                        "Set-Cookie",
                        "SESSION=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");

                redirect(exchange, "/login");
                return;
            }

            boolean sendMessage = path.equals("/messages/send");
            // HW7 Part B: add/edit forms for every record type
            boolean recordForm = RecordPages.isForm(path);

            String allowedMethod = sendMessage ? "POST"
                    : recordForm ? "GET, POST" : "GET";
            if (!List.of(allowedMethod.split(", ")).contains(method)) {
                methodNotAllowed(exchange, allowedMethod);
                return;
            }

            UserAccount user = currentUser(sessionToken(exchange));

            if (user == null) {
                if (path.equals("/")) {
                    send(exchange, 200, loginForm(""));
                } else {
                    redirect(exchange, "/login");
                }

                return;
            }

            if (sendMessage) {
                sendMessage(exchange, user);
                return;
            }

            if (recordForm && !user.isAdmin()) {
                send(exchange, 403,
                        view.renderMessage("Administrator access required."));
                return;
            }

            if (recordForm || RecordPages.isDetail(path)) {
                showRecordPage(exchange, method, path, user);
                return;
            }

            boolean messageDetails = path.matches("/messages/[0-9]+");
            if (!messageDetails && !Set.of(
                    "/", "/agents", "/facilities", "/countries",
                    "/briefs", "/missions", "/audit", "/messages", "/messages/new"
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

            String body;
            if (messageDetails) {
                if (!(pages instanceof WebPages webPages)) {
                    send(exchange, 500, view.renderMessage("Messaging is unavailable."));
                    return;
                }

                int messageId;
                try {
                    messageId = Integer.parseInt(path.substring("/messages/".length()));
                } catch (NumberFormatException e) {
                    messageId = 0;
                }
                if (messageId <= 0) {
                    send(exchange, 404, view.renderMessage("Message not found."));
                    return;
                }

                title = "Message";
                body = webPages.renderMessageDetails(messageId, user);
            } else {
                body = pages.render(path, user);
            }

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

    // The logged-in user for this token, or null. Every valid request resets the
    // session's 30 minute timer. A timed out or deleted token is no longer valid.
    private UserAccount currentUser(String token) {
        String username = token == null ? null : sessions.touch(token);

        if (username == null) {
            if (token != null) {
                users.remove(token);
            }
            return null;
        }

        UserAccount user = users.get(token);

        if (user == null) {
            // A session that was started before this server was restarted
            user = accounts.findUser(username);

            if (user != null) {
                users.put(token, user);
            }
        }

        return user;
    }

    // HW7: one record's details, or an add/edit form and its saving
    private void showRecordPage(HttpExchange exchange, String method,
                                String path, UserAccount user) throws Exception {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();

        if (query != null) {
            params.putAll(decodeForm(query.getBytes(StandardCharsets.UTF_8)));
        }

        if (method.equals("POST")) {
            byte[] bytes = exchange.getRequestBody().readNBytes(8193);

            if (bytes.length > 8192) {
                send(exchange, 413, view.renderMessage("Form too large."));
                return;
            }

            params.putAll(decodeForm(bytes));
        }

        PageResult result = pages.handle(method, path, params, user);

        if (result == null) {
            send(exchange, 404, view.renderMessage("Page not found."));
        } else if (result.isRedirect()) {
            redirect(exchange, result.redirect());
        } else {
            send(exchange, result.status(), view.renderPage(
                    result.title(), result.body(), user.isAdmin()));
        }
    }

    private void sendStylesheet(HttpExchange exchange) throws IOException {
        byte[] bytes = WebLayout.stylesheet().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/css; charset=UTF-8");

        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private void sendMessage(HttpExchange exchange, UserAccount user) throws IOException {
        if (!(pages instanceof WebPages webPages)) {
            send(exchange, 500, view.renderMessage("Messaging is unavailable."));
            return;
        }

        byte[] bytes = exchange.getRequestBody().readNBytes(8193);
        if (bytes.length > 8192) {
            send(exchange, 413, view.renderMessage("Message is too large."));
            return;
        }

        try {
            Map<String, String> form = decodeForm(bytes);
            String agentName = form.getOrDefault("agentName", "").strip();
            String messageText = form.getOrDefault("text", "").strip();
            if (agentName.isEmpty() || messageText.isEmpty()) {
                send(exchange, 400,
                        view.renderMessage("Please select an agent and enter a message."));
                return;
            }
            webPages.sendMessage(user, agentName, messageText);
        } catch (IllegalArgumentException e) {
            send(exchange, 400, view.renderMessage("Invalid recipient or message."));
            return;
        }
        redirect(exchange, "/messages");
    }

    private void login(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(8193);

        if (bytes.length > 8192) {
            send(exchange, 413, loginForm("Form too large."));
            return;
        }

        Map<String, String> form = decodeForm(bytes);

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
            sessions.delete(oldToken);
            users.remove(oldToken);
        }

        String token = sessions.create(user.getUsername());
        users.put(token, user);

        // No Expires or Max-Age: this is a browser-session cookie.
        exchange.getResponseHeaders().add(
                "Set-Cookie",
                "SESSION=" + token
                        + "; Path=/; HttpOnly; SameSite=Lax");

        redirect(exchange, "/");
    }

    private Map<String, String> decodeForm(byte[] bytes) {
        Map<String, String> form = new HashMap<>();
        String body = new String(bytes, StandardCharsets.UTF_8);
        for (String field : body.split("&")) {
            String[] pair = field.split("=", 2);
            String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length == 2
                    ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                    : "";
            form.put(name, value);
        }
        return form;
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
                    <link rel="stylesheet" href="/style.css">
                </head>
                <body>
                <main>
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
                </main>
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
