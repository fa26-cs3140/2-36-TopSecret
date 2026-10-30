package view;

import java.util.List;

public class HtmlView {

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
                + content
                + "</body>\n</html>";
    }

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

    public String renderTable(List<String> headers,
                              List<List<String>> rows) {
        StringBuilder html = new StringBuilder();
        html.append("<table border=\"1\">\n<tr>");

        for (String header : headers) {
            html.append("<th>")
                    .append(escape(header))
                    .append("</th>");
        }

        html.append("</tr>\n");

        for (List<String> row : rows) {
            html.append("<tr>");

            for (String cell : row) {
                html.append("<td>")
                        .append(escape(cell))
                        .append("</td>");
            }

            html.append("</tr>\n");
        }

        html.append("</table>\n");
        return html.toString();
    }

    public String renderText(String text) {
        return "<pre>" + escape(text) + "</pre>\n";
    }

    public String renderMessage(String message) {
        return "<p>" + escape(message) + "</p>\n";
    }

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
