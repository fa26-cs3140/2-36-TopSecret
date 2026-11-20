package view;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import model.Country;
import model.Facility;
import org.junit.jupiter.api.Test;
import view.CountryViews;

// HW7 Member A: how countries show up in the web view.
// Contract (CountryViews, pure functions that return a full page built with WebLayout.page):
//   static String facilityPage(Facility f, Country country)   country may be null (no country)
//   static String countryListPage(List<Country> countries)
// URL scheme assumed: /countries/{id}.
// The related lists (a country's facilities, a facility's agents and missions) are Member C's.
class CountryViewsTest {

    private final Facility oslo = new Facility(3, "Oslo Station", "OSL", 5);
    private final Country norway = new Country(5, "Norway");

    // ---------- Viewing a facility shows its country ----------

    @Test
    void facilityPageShowsTheFacility() {
        String html = CountryViews.facilityPage(oslo, norway);
        assertTrue(html.contains("Oslo Station"));
        assertTrue(html.contains("OSL"));
    }

    @Test
    void facilityPageShowsTheRelatedCountry() {
        String html = CountryViews.facilityPage(oslo, norway);
        assertTrue(html.contains("Country"));
        assertTrue(html.contains("Norway"));
    }

    @Test
    void facilityPageLinksToTheCountry() {
        String html = CountryViews.facilityPage(oslo, norway);
        assertTrue(html.contains("href=\"/countries/5\""));
    }

    @Test
    void facilityWithNoCountrySaysSo() {
        String html = CountryViews.facilityPage(new Facility(4, "Langley", "LNG"), null);
        assertTrue(html.contains("No country"));
        assertFalse(html.contains("/countries/"));
    }

    @Test
    void facilityPageEscapesTheCountryName() {
        String html = CountryViews.facilityPage(oslo, new Country(5, "<script>x</script>"));
        assertFalse(html.contains("<script>x"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void facilityPageEscapesTheFacilityName() {
        String html = CountryViews.facilityPage(
                new Facility(3, "<b>Bold</b>", "BLD", 5), norway);
        assertFalse(html.contains("<b>Bold"));
        assertTrue(html.contains("&lt;b&gt;Bold"));
    }

    @Test
    void facilityPageUsesTheSharedLayout() {
        String html = CountryViews.facilityPage(oslo, norway);
        assertTrue(html.contains("<nav"));
        assertTrue(html.contains("href=\"" + WebLayout.STYLESHEET_PATH + "\""));
        assertTrue(html.contains("Log out"));
    }

    // ---------- The list of countries ----------

    @Test
    void countryListShowsEveryCountryAsALink() {
        String html = CountryViews.countryListPage(List.of(norway, new Country(6, "Chile")));
        assertTrue(html.contains("Norway"));
        assertTrue(html.contains("Chile"));
        assertTrue(html.contains("href=\"/countries/5\""));
        assertTrue(html.contains("href=\"/countries/6\""));
    }

    @Test
    void emptyCountryListSaysSo() {
        String html = CountryViews.countryListPage(List.of());
        assertTrue(html.contains("No countries"));
    }

    @Test
    void countryListEscapesNames() {
        String html = CountryViews.countryListPage(List.of(new Country(5, "<i>Nor</i>")));
        assertFalse(html.contains("<i>Nor"));
        assertTrue(html.contains("&lt;i&gt;Nor"));
    }

    @Test
    void countryListUsesTheSharedLayout() {
        String html = CountryViews.countryListPage(List.of(norway));
        assertTrue(html.contains("<nav"));
        assertTrue(html.contains("href=\"" + WebLayout.STYLESHEET_PATH + "\""));
    }

    // ---------- Bodies without the page around them ----------

    @Test
    void facilityBodyHasDetailsButNoMenu() {
        String html = CountryViews.facilityBody(
                new model.Facility(3, "Oslo Station", "OSL", 5), new model.Country(5, "Norway"));

        assertTrue(html.contains("Oslo Station"));
        assertTrue(html.contains("href=\"/countries/5\""));
        assertFalse(html.contains("<nav"));
    }

    @Test
    void countryListBodyHasLinksButNoMenu() {
        String html = CountryViews.countryListBody(
                java.util.List.of(new model.Country(5, "Norway")));

        assertTrue(html.contains("href=\"/countries/5\""));
        assertFalse(html.contains("<nav"));
    }
}
