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
    void rendersAgents() {
        String html = view.renderAgents(
                List.of(List.of(
                        "1", "Ghost", "1980-01-01",
                        "", "Field Agent", "2"
                ))
        );

        assertTrue(html.contains("<td>Ghost</td>"));
        assertTrue(html.contains("<th>Facility ID</th>"));
    }

    @Test
    void rendersFacilities() {
        String html = view.renderFacilities(
                List.of(List.of("1", "Headquarters", "HQ"))
        );

        assertTrue(html.contains("<td>Headquarters</td>"));
        assertTrue(html.contains("<td>HQ</td>"));
    }

    @Test
    void rendersBriefs() {
        String html = view.renderBriefs(
                List.of(List.of(
                        "1", "Operation Alpha",
                        "2026-10-30", "Mission details"
                ))
        );

        assertTrue(html.contains("Operation Alpha"));
        assertTrue(html.contains("Mission details"));
    }

    @Test
    void rendersMissions() {
        String html = view.renderMissions(
                List.of(List.of(
                        "1", "Operation Alpha",
                        "Confidential", "2026-10-30",
                        "2", "3, 4"
                ))
        );

        assertTrue(html.contains("Operation Alpha"));
        assertTrue(html.contains("<td>3, 4</td>"));
    }

    @Test
    void rendersAuditLog() {
        String html = view.renderAuditLog(
                List.of("Ghost READ Brief #1",
                        "Admin UPDATE Agent #2")
        );

        assertTrue(html.contains("Ghost READ Brief #1"));
        assertTrue(html.contains("Admin UPDATE Agent #2"));
    }

    @Test
    void handlesEmptyAuditLog() {
        String html = view.renderAuditLog(List.of());

        assertTrue(html.contains(
                "No audit entries available."));
    }

    @Test
    void hidesAuditLogForStandardUsers() {
        String html = view.navigation(false);

        assertFalse(html.contains("Audit Log"));
    }

    @Test
    void showsAuditLogForAdmins() {
        String html = view.navigation(true);

        assertTrue(html.contains("Audit Log"));
    }

    @Test
    void escapesUnsafeHtml() {
        String html = view.escape("<script>alert(1)</script>");

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void rendersTextSafely() {
        String html = view.renderText("Secret <data>");

        assertTrue(html.contains("Secret &lt;data&gt;"));
    }

    @Test
    void handlesNullValues() {
        assertEquals("", view.escape(null));
        assertTrue(view.renderPage("Home", null, false)
                .contains("<h2>Home</h2>"));
    }
}