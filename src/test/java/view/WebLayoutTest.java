package view;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import view.WebLayout;

// HW7 Member A: the shared look of every web page.
// Contract (WebLayout, no web server needed, just strings):
//   static String page(String title, String bodyHtml)   a complete HTML document with the menu on it
//   static String menu()                                the <nav> with links and Log out
//   static String stylesheet()                          the CSS served at WebLayout.STYLESHEET_PATH
//   static final String STYLESHEET_PATH = "/style.css"
// Rules from the assignment: CSS only. No JavaScript and no images.
class WebLayoutTest {

    private static final Pattern INLINE_HANDLER =
            Pattern.compile("\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);

    // ---------- The page ----------

    @Test
    void pageIsACompleteHtmlDocument() {
        String html = WebLayout.page("Agents", "<p>hello</p>");
        String lower = html.toLowerCase(Locale.ROOT);
        assertTrue(lower.startsWith("<!doctype html>"));
        assertTrue(lower.contains("<html"));
        assertTrue(lower.contains("<head>"));
        assertTrue(lower.contains("<body"));
        assertTrue(lower.contains("</html>"));
    }

    @Test
    void pageHasTheTitle() {
        assertTrue(WebLayout.page("Agents", "").contains("<title>Agents"));
    }

    @Test
    void pageTitleIsEscaped() {
        String html = WebLayout.page("<b>x</b>", "");
        assertFalse(html.contains("<title><b>"));
        assertTrue(html.contains("&lt;b&gt;"));
    }

    @Test
    void pageKeepsTheBodyHtmlAsGiven() {
        String html = WebLayout.page("T", "<p class=\"x\">hello</p>");
        assertTrue(html.contains("<p class=\"x\">hello</p>"));
    }

    @Test
    void pageLinksTheStylesheet() {
        String html = WebLayout.page("T", "");
        assertEquals("/style.css", WebLayout.STYLESHEET_PATH);
        assertTrue(html.contains("rel=\"stylesheet\""));
        assertTrue(html.contains("href=\"" + WebLayout.STYLESHEET_PATH + "\""));
    }

    @Test
    void menuAppearsBeforeTheBodyContent() {
        String html = WebLayout.page("T", "<p>BODY-MARKER</p>");
        int nav = html.indexOf("<nav");
        int body = html.indexOf("BODY-MARKER");
        assertTrue(nav >= 0);
        assertTrue(nav < body);
    }

    @Test
    void everyPageIsBuiltWithTheMenu() {
        List<String> pages = List.of(
                WebLayout.page("Home", ""),
                WebLayout.page("Agents", "<p>a</p>"),
                WebLayout.page("Facilities", "<p>b</p>"),
                WebLayout.page("", ""));
        for (String page : pages) {
            assertTrue(page.contains("<nav"), "page without a menu: " + page);
            assertTrue(page.contains("Log out"));
        }
    }

    // ---------- The menu ----------

    @Test
    void menuLinksToEachSectionOfTheSystem() {
        String menu = WebLayout.menu();
        for (String path : List.of("/", "/agents", "/facilities", "/missions", "/briefs", "/countries")) {
            assertTrue(menu.contains("href=\"" + path + "\""), "menu is missing " + path);
        }
    }

    @Test
    void menuHasALogOutFeature() {
        String menu = WebLayout.menu();
        assertTrue(menu.contains("Log out"));
        assertTrue(menu.contains("/logout"));
    }

    @Test
    void menuIsInsideANavElement() {
        String menu = WebLayout.menu();
        assertTrue(menu.contains("<nav"));
        assertTrue(menu.contains("</nav>"));
    }

    // ---------- No JavaScript, no images ----------

    @Test
    void pageHasNoJavaScript() {
        String html = WebLayout.page("T", "<p>x</p>").toLowerCase(Locale.ROOT);
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("javascript:"));
        assertFalse(INLINE_HANDLER.matcher(html).find());
    }

    @Test
    void pageHasNoImages() {
        String html = WebLayout.page("T", "<p>x</p>").toLowerCase(Locale.ROOT);
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("<svg"));
        assertFalse(html.contains("<picture"));
        assertFalse(html.contains("<canvas"));
        assertFalse(html.contains("rel=\"icon\""));
    }

    @Test
    void menuHasNoJavaScriptOrImages() {
        String menu = WebLayout.menu().toLowerCase(Locale.ROOT);
        assertFalse(menu.contains("<script"));
        assertFalse(menu.contains("<img"));
        assertFalse(INLINE_HANDLER.matcher(menu).find());
    }

    // ---------- The stylesheet ----------

    @Test
    void stylesheetIsNotEmpty() {
        assertFalse(WebLayout.stylesheet().isBlank());
    }

    @Test
    void stylesheetStylesTheMenuAndBody() {
        String css = WebLayout.stylesheet();
        assertTrue(css.contains("nav"));
        assertTrue(css.contains("body"));
    }

    @Test
    void stylesheetLooksLikeCss() {
        String css = WebLayout.stylesheet();
        assertTrue(css.contains("{"));
        assertTrue(css.contains("}"));
        assertTrue(css.contains(":"));
        assertEquals(count(css, '{'), count(css, '}'), "unbalanced braces");
    }

    @Test
    void stylesheetUsesNoImagesOrOutsideFiles() {
        String css = WebLayout.stylesheet().toLowerCase(Locale.ROOT);
        assertFalse(css.contains("url("));
        assertFalse(css.contains("@import"));
        assertFalse(css.contains("<script"));
        assertFalse(css.contains("expression("));
    }

    private static long count(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }
}