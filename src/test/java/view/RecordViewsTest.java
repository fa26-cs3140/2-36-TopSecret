package view;

import org.junit.jupiter.api.Test;
import view.RecordViews.Input;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// The HTML building blocks used by the record pages
class RecordViewsTest {

    private final RecordViews views = new RecordViews();

    @Test
    void linkEscapesItsText() {
        assertEquals("<a href=\"/agents/7\">A &amp; B</a>", views.link("/agents/7", "A & B"));
    }

    @Test
    void textEscapesAndShowsNoneForEmptyValues() {
        assertEquals("&lt;b&gt;", views.text("<b>"));
        assertTrue(views.text(null).contains("None"));
        assertTrue(views.text("  ").contains("None"));
    }

    @Test
    void detailsListsLabelsAndValuesInOrder() {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Name", views.text("Ghost"));
        rows.put("Facility", views.link("/facilities/1", "Langley"));

        String html = views.details(rows);

        assertTrue(html.contains("<dt>Name</dt><dd>Ghost</dd>"));
        assertTrue(html.contains("<dd><a href=\"/facilities/1\">Langley</a></dd>"));
        assertTrue(html.indexOf("Name") < html.indexOf("Facility"));
    }

    @Test
    void relatedShowsEachItemOrNone() {
        String filled = views.related("Missions", List.of(views.link("/missions/1", "Skyfall")));
        String empty = views.related("Missions", List.of());

        assertTrue(filled.contains("<h3>Missions</h3>"));
        assertTrue(filled.contains("<li><a href=\"/missions/1\">Skyfall</a></li>"));
        assertTrue(empty.contains("None"));
        assertFalse(empty.contains("<ul>"));
    }

    @Test
    void errorIsEmptyWithoutAMessage() {
        assertEquals("", views.error(null));
        assertEquals("", views.error(" "));
        assertTrue(views.error("Name is required").contains("class=\"error\""));
        assertTrue(views.error("<x>").contains("&lt;x&gt;"));
    }

    @Test
    void formPostsToItsActionWithEveryInput() {
        Map<String, String> facilities = new LinkedHashMap<>();
        facilities.put("1", "Langley");
        facilities.put("2", "Harbor <2>");

        String html = views.form("/agents/edit?id=7", List.of(
                Input.text("name", "Name", "Gh\"ost", true),
                Input.area("notes", "Notes", "<secret>", false),
                Input.select("facilityId", "Facility", "2", true, facilities)));

        assertTrue(html.contains("<form method=\"post\" action=\"/agents/edit?id=7\">"));
        assertTrue(html.contains("name=\"name\" value=\"Gh&quot;ost\" required"));
        assertTrue(html.contains("<textarea id=\"notes\" name=\"notes\" rows=\"6\">&lt;secret&gt;</textarea>"));
        assertTrue(html.contains("<option value=\"1\">Langley</option>"));
        assertTrue(html.contains("<option value=\"2\" selected>Harbor &lt;2&gt;</option>"));
        assertTrue(html.contains("<button type=\"submit\">Save</button>"));
    }

    @Test
    void emptyFormStillHasASaveButton() {
        String html = views.form("/countries/new", List.of(Input.text("name", "Name", "", true)));

        assertTrue(html.contains("value=\"\""));
        assertTrue(html.contains("Save"));
        assertFalse(html.contains("<script"));
    }
}
