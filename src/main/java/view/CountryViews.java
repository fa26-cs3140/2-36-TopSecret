package view;

import model.Country;
import model.Facility;

import java.util.List;

// Web pages that show countries (HW7, Member A, View).
// Each method returns a complete page built with WebLayout, so it has the menu and styling.
// Links use /countries/{id}. The related lists on these pages are filled in by the model role.
public class CountryViews {

    private CountryViews() {
    }

    // A facility with its country. The country may be null when the facility has none.
    public static String facilityPage(Facility facility, Country country) {
        return WebLayout.page(facility.getName(), facilityBody(facility, country));
    }

    // The same facility details without the page around them
    public static String facilityBody(Facility facility, Country country) {
        String name = WebLayout.escape(facility.getName());
        String countryHtml = country == null
                ? "<span class=\"empty\">No country</span>"
                : "<a href=\"/countries/" + country.getId() + "\">"
                + WebLayout.escape(country.getName()) + "</a>";

        return "<h1>" + name + "</h1>\n"
                + "<dl>\n"
                + "<dt>Name</dt><dd>" + name + "</dd>\n"
                + "<dt>Abbreviation</dt><dd>" + WebLayout.escape(facility.getAbbreviation()) + "</dd>\n"
                + "<dt>Country</dt><dd>" + countryHtml + "</dd>\n"
                + "</dl>";
    }

    // Every country as a link to its page
    public static String countryListPage(List<Country> countries) {
        return WebLayout.page("Countries", countryListBody(countries));
    }

    // The same list without the page around it
    public static String countryListBody(List<Country> countries) {
        StringBuilder body = new StringBuilder("<h1>Countries</h1>\n");
        if (countries.isEmpty()) {
            body.append("<p class=\"empty\">No countries</p>");
        } else {
            body.append("<ul>\n");
            for (Country c : countries) {
                body.append("<li><a href=\"/countries/").append(c.getId()).append("\">")
                        .append(WebLayout.escape(c.getName())).append("</a></li>\n");
            }
            body.append("</ul>");
        }
        return body.toString();
    }
}
