package view;
import java.util.List;
public class HtmlView {

    // Creates a complete HTML page.
    public String renderPage(String title, String content,
                             boolean admin) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head><meta charset=\"UTF-8\">"
                + "<title>" + escape(title) + "</title></head>\n"
                + "<body>\n"
                + "<h1>TOP SECRET SYSTEM</h1>\n"
                + navigation(admin)
                + "<h2>" + escape(title) + "</h2>\n"
                + (content == null ? "" : content)
                + "\n</body>\n</html>";
    }

    // Navigation links for the web interface.
    public String navigation(boolean admin) {
        StringBuilder html = new StringBuilder();

        html.append("<nav>");
        html.append("<a href=\"/\">Home</a> | ");
        html.append("<a href=\"/briefs\">Briefs</a> | ");
        html.append("<a href=\"/agents\">Agents</a> | ");
        html.append("<a href=\"/facilities\">Facilities</a> | ");
        html.append("<a href=\"/missions\">Missions</a>");

        if (admin) {
            html.append(" | <a href=\"/audit\">Audit Log</a>");
        }

        html.append("</nav><hr>\n");
        return html.toString();
    }

    // Reusable HTML table for database records.
    public String renderTable(List<String> headers,
                              List<List<String>> rows) {
        StringBuilder html = new StringBuilder();

        html.append("<table border=\"1\">\n<thead><tr>");

        for (String header : headers) {
            html.append("<th>")
                    .append(escape(header))
                    .append("</th>");
        }

        html.append("</tr></thead>\n<tbody>\n");

        for (List<String> row : rows) {
            html.append("<tr>");

            for (String cell : row) {
                html.append("<td>")
                        .append(escape(cell))
                        .append("</td>");
            }

            html.append("</tr>\n");
        }

        html.append("</tbody>\n</table>\n");
        return html.toString();
    }

    // Displays agents.
    // Columns: ID, Name, Birth, Death, Notes, Facility ID.
    public String renderAgents(List<List<String>> agents) {
        return renderTable(
                List.of("ID", "Name", "Date of Birth",
                        "Date of Death", "Notes", "Facility ID"),
                agents
        );
    }

    // Displays facilities.
    // Columns: ID, Name, Abbreviation.
    public String renderFacilities(List<List<String>> facilities) {
        return renderTable(
                List.of("ID", "Name", "Abbreviation"),
                facilities
        );
    }

    // Displays mission briefs.
    // Columns: ID, Title, Date, Text.
    public String renderBriefs(List<List<String>> briefs) {
        return renderTable(
                List.of("ID", "Title", "Date", "Text"),
                briefs
        );
    }

    // Displays missions and their assignments.
    // Columns: ID, Title, Brief, Date, Facility ID, Agent IDs.
    public String renderMissions(List<List<String>> missions) {
        return renderTable(
                List.of("ID", "Title", "Brief", "Date",
                        "Facility ID", "Agent IDs"),
                missions
        );
    }

    // Displays the audit log as a read-only list.
    public String renderAuditLog(List<String> entries) {
        StringBuilder html = new StringBuilder();

        html.append("<h3>Audit Log</h3>\n");

        if (entries == null || entries.isEmpty()) {
            return html.append(
                    "<p>No audit entries available.</p>\n"
            ).toString();
        }

        html.append("<ul>\n");

        for (String entry : entries) {
            html.append("<li>")
                    .append(escape(entry))
                    .append("</li>\n");
        }

        html.append("</ul>\n");
        return html.toString();
    }

    // Displays a read-only record's text.
    public String renderText(String text) {
        return "<pre>" + escape(text) + "</pre>\n";
    }

    // Displays an informational message.
    public String renderMessage(String message) {
        return "<p>" + escape(message) + "</p>\n";
    }

    // Converts unsafe characters into HTML entities.
    public String escape(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}