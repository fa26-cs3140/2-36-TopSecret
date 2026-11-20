package view;

import java.util.List;
import java.util.Map;

// Small HTML building blocks for record pages (HW7): a details list, a list of
// related links, and an add/edit form. Everything passed in as plain text is
// escaped. Styling comes from the shared stylesheet in WebLayout.
public class RecordViews {

    // One input of a form. Kind is TEXT, AREA (large text box) or SELECT.
    public record Input(String kind, String name, String label, String value,
                        boolean required, Map<String, String> options) {

        public static Input text(String name, String label, String value, boolean required) {
            return new Input("TEXT", name, label, value, required, Map.of());
        }

        public static Input area(String name, String label, String value, boolean required) {
            return new Input("AREA", name, label, value, required, Map.of());
        }

        // options maps each value to the text shown for it
        public static Input select(String name, String label, String value,
                                   boolean required, Map<String, String> options) {
            return new Input("SELECT", name, label, value, required, options);
        }
    }

    // A link to another page
    public String link(String href, String text) {
        return "<a href=\"" + WebLayout.escape(href) + "\">" + WebLayout.escape(text) + "</a>";
    }

    // Label and value pairs. Labels are plain text. Values are HTML, so callers
    // use text() for plain values and link() for links.
    public String details(Map<String, String> labelToValueHtml) {
        StringBuilder html = new StringBuilder("<dl>\n");
        for (Map.Entry<String, String> row : labelToValueHtml.entrySet()) {
            html.append("<dt>").append(WebLayout.escape(row.getKey())).append("</dt><dd>")
                    .append(row.getValue()).append("</dd>\n");
        }
        return html.append("</dl>\n").toString();
    }

    // Plain text made safe for HTML. Empty values show as "None".
    public String text(String value) {
        return value == null || value.isBlank()
                ? "<span class=\"empty\">None</span>"
                : WebLayout.escape(value);
    }

    // A heading and a list of related items (already HTML, usually links)
    public String related(String heading, List<String> itemHtml) {
        StringBuilder html = new StringBuilder("<h3>")
                .append(WebLayout.escape(heading)).append("</h3>\n");
        if (itemHtml.isEmpty()) {
            return html.append("<p class=\"empty\">None</p>\n").toString();
        }
        html.append("<ul>\n");
        for (String item : itemHtml) {
            html.append("<li>").append(item).append("</li>\n");
        }
        return html.append("</ul>\n").toString();
    }

    // A message shown above a form when the input was not accepted
    public String error(String message) {
        return message == null || message.isBlank()
                ? ""
                : "<p class=\"error\">" + WebLayout.escape(message) + "</p>\n";
    }

    // An add or edit form that posts back to the given address
    public String form(String action, List<Input> inputs) {
        StringBuilder html = new StringBuilder("<form method=\"post\" action=\"")
                .append(WebLayout.escape(action)).append("\">\n");
        for (Input input : inputs) {
            String name = WebLayout.escape(input.name());
            String need = input.required() ? " required" : "";
            html.append("<label for=\"").append(name).append("\">")
                    .append(WebLayout.escape(input.label())).append("</label>\n");
            switch (input.kind()) {
                case "AREA" -> html.append("<textarea id=\"").append(name).append("\" name=\"")
                        .append(name).append("\" rows=\"6\"").append(need).append(">")
                        .append(WebLayout.escape(input.value())).append("</textarea>\n");
                case "SELECT" -> html.append(select(input, name, need));
                default -> html.append("<input type=\"text\" id=\"").append(name)
                        .append("\" name=\"").append(name).append("\" value=\"")
                        .append(WebLayout.escape(input.value())).append("\"").append(need)
                        .append(">\n");
            }
        }
        return html.append("<button type=\"submit\">Save</button>\n</form>\n").toString();
    }

    private String select(Input input, String name, String need) {
        StringBuilder html = new StringBuilder("<select id=\"").append(name)
                .append("\" name=\"").append(name).append("\"").append(need).append(">\n")
                .append("<option value=\"\">Choose one</option>\n");
        for (Map.Entry<String, String> option : input.options().entrySet()) {
            html.append("<option value=\"").append(WebLayout.escape(option.getKey())).append("\"")
                    .append(option.getKey().equals(input.value()) ? " selected" : "")
                    .append(">").append(WebLayout.escape(option.getValue())).append("</option>\n");
        }
        return html.append("</select>\n").toString();
    }
}
