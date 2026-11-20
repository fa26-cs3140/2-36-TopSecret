package view;
import model.Message;
import java.util.List;
public class HtmlView {

    // Creates a complete HTML page with the shared menu and stylesheet.
    public String renderPage(String title, String content,
                             boolean admin) {
        return WebLayout.page(title,
                "<h2>" + escape(title) + "</h2>\n"
                        + (content == null ? "" : content),
                admin);
    }

    // Navigation links for the web interface.
    public String navigation(boolean admin) {
        return WebLayout.menu(admin);
    }

    // Like renderTable, but the second cell of each row (the name or title)
    // links to that record's page: basePath + "/" + the id in the first cell.
    public String renderLinkedTable(List<String> headers,
                                    List<List<String>> rows,
                                    String basePath) {
        StringBuilder html = new StringBuilder();

        html.append("<table>\n<thead><tr>");

        for (String header : headers) {
            html.append("<th>").append(escape(header)).append("</th>");
        }

        html.append("</tr></thead>\n<tbody>\n");

        for (List<String> row : rows) {
            html.append("<tr>");

            for (int i = 0; i < row.size(); i++) {
                html.append("<td>");

                if (i == 1) {
                    html.append("<a href=\"").append(escape(basePath))
                            .append("/").append(escape(row.get(0))).append("\">")
                            .append(escape(row.get(i))).append("</a>");
                } else {
                    html.append(escape(row.get(i)));
                }

                html.append("</td>");
            }

            html.append("</tr>\n");
        }

        html.append("</tbody>\n</table>\n");
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

    // HW7 Part C: Display the logged-in agent's inbox.
    public String renderInbox(List<Message> messages) {
        StringBuilder html = new StringBuilder();

        html.append("<h3>My Messages</h3>\n");
        html.append("<p><a href=\"/messages/new\">Compose Message</a></p>\n");

        if (messages == null || messages.isEmpty()) {
            html.append("<p>No messages available.</p>\n");
            return html.toString();
        }

        html.append("<table border=\"1\">\n");
        html.append("<tr><th>Status</th><th>From</th>");
        html.append("<th>Date</th><th>Message</th></tr>\n");

        for (Message message : messages) {
            html.append("<tr>");

            html.append("<td>")
                    .append(message.isRead() ? "Read" : "Unread")
                    .append("</td>");

            html.append("<td>")
                    .append(escape(message.getSender()))
                    .append("</td>");

            html.append("<td>")
                    .append(escape(message.getSentAt()))
                    .append("</td>");

            html.append("<td><a href=\"/messages/")
                    .append(message.getId())
                    .append("\">View Message</a></td>");

            html.append("</tr>\n");
        }

        html.append("</table>\n");
        return html.toString();
    }

    // HW7 Part C: Display an individual message.
    public String renderMessageDetails(Message message) {
        if (message == null) {
            return renderMessage("Message not found.");
        }

        StringBuilder html = new StringBuilder();

        html.append("<h3>Message Details</h3>\n");

        html.append("<p><strong>From:</strong> ")
                .append(escape(message.getSender()))
                .append("</p>\n");

        html.append("<p><strong>To:</strong> ")
                .append(escape(message.getAgentName()))
                .append("</p>\n");

        html.append("<p><strong>Date:</strong> ")
                .append(escape(message.getSentAt()))
                .append("</p>\n");

        html.append("<h4>Message</h4>\n");

        html.append("<pre>")
                .append(escape(message.getText()))
                .append("</pre>\n");

        html.append("<p><a href=\"/messages\">Back to Inbox</a></p>\n");

        return html.toString();
    }

    // HW7 Part C: Form for sending a new message.
    public String renderComposeMessage(List<String> agentNames) {
        StringBuilder html = new StringBuilder();

        html.append("<h3>Compose Message</h3>\n");

        html.append("<form method=\"POST\" action=\"/messages/send\">\n");

        html.append("<label for=\"agentName\">Recipient:</label>\n");
        html.append("<select id=\"agentName\" name=\"agentName\" required>\n");

        html.append("<option value=\"\">Select an agent</option>\n");

        if (agentNames != null) {
            for (String name : agentNames) {
                html.append("<option value=\"")
                        .append(escape(name))
                        .append("\">")
                        .append(escape(name))
                        .append("</option>\n");
            }
        }

        html.append("</select><br><br>\n");

        html.append("<label for=\"text\">Message:</label><br>\n");

        html.append("<textarea id=\"text\" name=\"text\" ")
                .append("rows=\"6\" cols=\"50\" required></textarea>\n");

        html.append("<br><br>\n");

        html.append("<button type=\"submit\">Send Message</button>\n");

        html.append("</form>\n");

        html.append("<p><a href=\"/messages\">Back to Inbox</a></p>\n");

        return html.toString();
    }

    // HW7 Part B: Form for adding or editing a facility.
    public String renderFacilityForm(
            String action,
            String name,
            String abbreviation,
            String selectedCountryId,
            java.util.Map<String, String> countries) {

        StringBuilder html = new StringBuilder();

        html.append("<form method=\"post\" action=\"")
                .append(escape(action)).append("\">");

        html.append("<label>Name <input name=\"name\" value=\"")
                .append(escape(name)).append("\" required></label><br>");

        html.append("<label>Abbreviation <input name=\"abbreviation\" value=\"")
                .append(escape(abbreviation)).append("\" required></label><br>");

        html.append("<label>Country <select name=\"countryId\" required>")
                .append("<option value=\"\">Choose a country</option>");

        for (var country : countries.entrySet()) {
            html.append("<option value=\"")
                    .append(escape(country.getKey())).append("\"");

            if (country.getKey().equals(selectedCountryId)) {
                html.append(" selected");
            }

            html.append(">").append(escape(country.getValue()))
                    .append("</option>");
        }

        return html.append("</select></label><br>")
                .append("<button type=\"submit\">Save</button></form>")
                .toString();
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