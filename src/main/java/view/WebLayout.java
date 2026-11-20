package view;

// The shared look of every web page (HW7, Member A, View).
// Plain strings in, plain strings out: no web server is needed to use or test it.
// Rules from the assignment: CSS only, no JavaScript, no images.
public class WebLayout {

    // Where the web server should serve stylesheet() from
    public static final String STYLESHEET_PATH = "/style.css";

    private static final String SITE_NAME = "Top Secret";

    private WebLayout() {
    }

    // A complete HTML page: the menu first, then the body.
    // The title is plain text and is escaped. bodyHtml is already HTML and is used as given,
    // so callers must escape any user text they put in it (see escape).
    public static String page(String title, String bodyHtml) {
        return page(title, bodyHtml, false);
    }

    // Same page, with the admin-only menu links when admin is true
    public static String page(String title, String bodyHtml, boolean admin) {
        String heading = (title == null || title.isBlank())
                ? SITE_NAME : escape(title) + " - " + SITE_NAME;
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>" + heading + "</title>\n"
                + "<link rel=\"stylesheet\" href=\"" + STYLESHEET_PATH + "\">\n"
                + "</head>\n"
                + "<body>\n"
                + menu(admin) + "\n"
                + "<main>\n"
                + (bodyHtml == null ? "" : bodyHtml) + "\n"
                + "</main>\n"
                + "</body>\n"
                + "</html>\n";
    }

    // The menu shown on every page, with the log out link
    public static String menu() {
        return menu(false);
    }

    // Admins also get the audit log link
    public static String menu(boolean admin) {
        return "<nav>\n"
                + "<span class=\"brand\">" + SITE_NAME + "</span>\n"
                + "<a href=\"/\">Home</a>\n"
                + "<a href=\"/agents\">Agents</a>\n"
                + "<a href=\"/facilities\">Facilities</a>\n"
                + "<a href=\"/countries\">Countries</a>\n"
                + "<a href=\"/missions\">Missions</a>\n"
                + "<a href=\"/briefs\">Briefs</a>\n"
                + "<a href=\"/messages\">Messages</a>\n"
                + (admin ? "<a href=\"/audit\">Audit Log</a>\n" : "")
                + "<a class=\"logout\" href=\"/logout\">Log out</a>\n"
                + "</nav>";
    }

    // Makes user text safe to put inside HTML
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    // All the styling for the site. The menu stays at the top of the window while scrolling.
    // Includes table and form styles for the list and edit pages.
    public static String stylesheet() {
        return """
                :root {
                  --bg: #f4f5f7;
                  --panel: #ffffff;
                  --text: #1f2933;
                  --muted: #616e7c;
                  --accent: #1f4e79;
                  --accent-text: #ffffff;
                  --border: #d3d9e0;
                  --danger: #b42318;
                }

                * {
                  box-sizing: border-box;
                }

                body {
                  margin: 0;
                  background: var(--bg);
                  color: var(--text);
                  font-family: system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif;
                  line-height: 1.5;
                }

                nav {
                  position: sticky;
                  top: 0;
                  display: flex;
                  flex-wrap: wrap;
                  align-items: center;
                  gap: 0.25rem;
                  padding: 0.5rem 1.5rem;
                  background: var(--accent);
                }

                nav .brand {
                  margin-right: 1rem;
                  color: var(--accent-text);
                  font-weight: bold;
                  letter-spacing: 0.08em;
                  text-transform: uppercase;
                }

                nav a {
                  padding: 0.35rem 0.75rem;
                  border-radius: 4px;
                  color: var(--accent-text);
                  text-decoration: none;
                }

                nav a:hover,
                nav a:focus {
                  background: rgba(255, 255, 255, 0.2);
                }

                nav a.logout {
                  margin-left: auto;
                  border: 1px solid rgba(255, 255, 255, 0.6);
                }

                main {
                  max-width: 60rem;
                  margin: 1.5rem auto;
                  padding: 1.5rem;
                  background: var(--panel);
                  border: 1px solid var(--border);
                  border-radius: 6px;
                }

                h1 {
                  margin-top: 0;
                  color: var(--accent);
                }

                a {
                  color: var(--accent);
                }

                dl {
                  display: grid;
                  grid-template-columns: max-content 1fr;
                  gap: 0.5rem 1.5rem;
                }

                dt {
                  color: var(--muted);
                  font-weight: bold;
                }

                dd {
                  margin: 0;
                }

                ul {
                  padding-left: 1.25rem;
                }

                table {
                  width: 100%;
                  border-collapse: collapse;
                }

                th,
                td {
                  padding: 0.5rem 0.75rem;
                  border-bottom: 1px solid var(--border);
                  text-align: left;
                }

                th {
                  background: var(--bg);
                }

                .empty {
                  color: var(--muted);
                  font-style: italic;
                }

                form label {
                  display: block;
                  margin: 1rem 0 0.25rem;
                  font-weight: bold;
                }

                input[type="text"],
                input[type="password"],
                input[type="date"],
                select,
                textarea {
                  width: 100%;
                  padding: 0.5rem;
                  border: 1px solid var(--border);
                  border-radius: 4px;
                  font: inherit;
                }

                button,
                input[type="submit"] {
                  margin-top: 1rem;
                  padding: 0.5rem 1.25rem;
                  border: 0;
                  border-radius: 4px;
                  background: var(--accent);
                  color: var(--accent-text);
                  font: inherit;
                  cursor: pointer;
                }

                .error {
                  color: var(--danger);
                  font-weight: bold;
                }
                """;
    }
}