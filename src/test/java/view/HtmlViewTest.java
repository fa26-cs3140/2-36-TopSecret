package view;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HtmlViewTest {

    private final HtmlView view = new HtmlView();

    @Test
    void rendersHtmlPage() {
        String html = view.renderPage(
                "Agents", "<p>Agent List</p>", false);

        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<h2>Agents</h2>"));
        assertTrue(html.contains("Agent List"));
    }

    @Test
    void rendersTable() {
        String html = view.renderTable(
                List.of("ID", "Name"),
                List.of(List.of("1", "Ghost")));

        assertTrue(html.contains("<th>Name</th>"));
        assertTrue(html.contains("<td>Ghost</td>"));
    }

    @Test
    void escapesUnsafeHtml() {
        String result = view.escape("<script>alert(1)</script>");

        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("&lt;script&gt;"));
    }

    @Test
    void hidesAdminNavigationForStandardUser() {
        String html = view.navigation(false);

        assertFalse(html.contains("Audit Log"));
    }

    @Test
    void showsAdminNavigationForAdmin() {
        String html = view.navigation(true);

        assertTrue(html.contains("Audit Log"));
    }

    @Test
    void rendersPlainText() {
        String html = view.renderText("Secret <data>");

        assertTrue(html.contains("Secret &lt;data&gt;"));
    }

    @Test
    void handlesNullText() {
        assertEquals("", view.escape(null));
    }
}
