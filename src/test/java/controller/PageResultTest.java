package controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void pageIsOkWithTitleAndBody() {
        PageResult result = PageResult.page("Agents", "<p>list</p>");

        assertEquals(200, result.status());
        assertEquals("Agents", result.title());
        assertEquals("<p>list</p>", result.body());
        assertFalse(result.isRedirect());
    }

    @Test
    void errorKeepsItsStatus() {
        PageResult result = PageResult.error(404, "Not found", "<p>gone</p>");

        assertEquals(404, result.status());
        assertFalse(result.isRedirect());
    }

    @Test
    void redirectCarriesTheNewAddress() {
        PageResult result = PageResult.redirectTo("/agents/7");

        assertTrue(result.isRedirect());
        assertEquals("/agents/7", result.redirect());
        assertEquals(303, result.status());
    }
}
